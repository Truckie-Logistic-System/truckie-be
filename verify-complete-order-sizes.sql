-- Verify all order sizes with Vietnamese descriptions
SELECT 
    id,
    min_height,
    max_height,
    min_length,
    max_length,
    min_width,
    max_width,
    description,
    status
FROM order_sizes
WHERE status = 'ACTIVE'
ORDER BY 
    CASE 
        -- Small items
        WHEN max_length <= 0.30 AND max_width <= 0.20 THEN 1  -- MINI_PARCEL
        WHEN max_length <= 0.50 AND max_width <= 0.40 THEN 2  -- SMALL_PARCEL
        WHEN max_length <= 0.90 AND max_width <= 0.70 THEN 3  -- MEDIUM_BOX
        WHEN max_length <= 1.20 AND max_width <= 1.00 AND max_height <= 1.20 THEN 4  -- LARGE_BOX
        WHEN max_length <= 1.50 AND max_width <= 1.50 THEN 5  -- EXTRA_LARGE_BOX
        -- Pallets
        WHEN max_length <= 0.80 AND max_width <= 0.60 THEN 6  -- HALF_PALLET
        WHEN max_length <= 1.20 AND max_width <= 0.80 THEN 7  -- STANDARD_PALLET
        WHEN max_length <= 1.20 AND max_width <= 1.00 THEN 8  -- LARGE_PALLET
        WHEN max_length > 2.00 AND max_width > 1.00 THEN 9  -- DOUBLE_PALLET
        -- Special shapes
        WHEN max_height > 2.00 THEN 10  -- TALL_ITEM
        WHEN max_length > 2.00 AND max_width <= 0.50 THEN 11  -- LONG_ITEM
        WHEN max_height <= 0.15 AND max_length > 2.00 THEN 12  -- FLAT_ITEM
        WHEN max_height <= 0.30 AND max_length <= 2.00 AND max_width <= 0.30 THEN 13  -- CYLINDRICAL
        ELSE 14  -- IRREGULAR_SHAPE
    END,
    max_length DESC;

-- Count total active order sizes
SELECT COUNT(*) as total_order_sizes FROM order_sizes WHERE status = 'ACTIVE';

-- Check for any missing descriptions
SELECT 
    COUNT(*) as missing_descriptions
FROM order_sizes 
WHERE status = 'ACTIVE' 
  AND (description IS NULL OR description = '' OR description NOT LIKE '%Phù hợp%');
