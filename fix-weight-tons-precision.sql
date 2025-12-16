-- Fix weight_tons precision and recalculate data
-- Run this script directly in PostgreSQL to fix immediately

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
    id,
    tracking_code,
    weight_base_unit,
    unit,
    weight_tons,
    -- Expected weight_tons calculation
    CASE 
        WHEN unit = 'Kí' THEN weight_base_unit * 0.001
        WHEN unit = 'Yến' THEN weight_base_unit * 0.01
        WHEN unit = 'Tạ' THEN weight_base_unit * 0.1
        WHEN unit = 'Tấn' THEN weight_base_unit * 1
    END as expected_weight_tons
FROM order_details
WHERE weight_base_unit = 125 AND unit = 'Kí'
LIMIT 5;

-- Should show:
-- weight_base_unit = 125
-- unit = 'Kí'
-- weight_tons = 0.125000 (NOT 0.13)
-- expected_weight_tons = 0.125000
