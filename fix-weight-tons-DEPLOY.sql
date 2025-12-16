-- Fix weight_tons precision on DEPLOY database (14.225.253.8:5432/truckie)
-- Run: psql -h 14.225.253.8 -U postgres -d truckie -f fix-weight-tons-DEPLOY.sql

-- Step 1: Check current column type
SELECT 
    column_name, 
    data_type, 
    numeric_precision, 
    numeric_scale
FROM information_schema.columns 
WHERE table_name = 'order_details' AND column_name = 'weight_tons';

-- Step 2: Alter column to DECIMAL(19,6)
ALTER TABLE order_details 
ALTER COLUMN weight_tons TYPE DECIMAL(19,6);

-- Step 3: Recalculate weight_tons from weight_base_unit + unit for existing data
UPDATE order_details
SET weight_tons = CASE 
    WHEN unit = 'Kí' THEN weight_base_unit * 0.001
    WHEN unit = 'Yến' THEN weight_base_unit * 0.01
    WHEN unit = 'Tạ' THEN weight_base_unit * 0.1
    WHEN unit = 'Tấn' THEN weight_base_unit * 1
    ELSE weight_tons
END
WHERE weight_base_unit IS NOT NULL AND unit IS NOT NULL;

-- Step 4: Verify the fix
SELECT 
    tracking_code,
    weight_base_unit,
    unit,
    weight_tons,
    CASE 
        WHEN unit = 'Kí' THEN weight_base_unit * 0.001
        WHEN unit = 'Yến' THEN weight_base_unit * 0.01
        WHEN unit = 'Tạ' THEN weight_base_unit * 0.1
        WHEN unit = 'Tấn' THEN weight_base_unit * 1
    END as expected_weight_tons
FROM order_details
WHERE weight_base_unit = 125 AND unit = 'Kí'
ORDER BY created_at DESC
LIMIT 5;
