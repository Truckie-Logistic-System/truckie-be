-- Check current order_sizes data
SELECT 
    size_name,
    height_m,
    length_m,
    width_m,
    min_height_m,
    min_length_m,
    min_width_m,
    description,
    status,
    created_at
FROM order_sizes
ORDER BY 
    CASE size_name
        WHEN 'MINI_PARCEL' THEN 1
        WHEN 'SMALL_PARCEL' THEN 2
        WHEN 'MEDIUM_BOX' THEN 3
        WHEN 'LARGE_BOX' THEN 4
        WHEN 'EXTRA_LARGE_BOX' THEN 5
        WHEN 'HALF_PALLET' THEN 6
        WHEN 'STANDARD_PALLET' THEN 7
        WHEN 'LARGE_PALLET' THEN 8
        WHEN 'DOUBLE_PALLET' THEN 9
        WHEN 'LONG_ITEM' THEN 10
        WHEN 'TALL_ITEM' THEN 11
        WHEN 'FLAT_ITEM' THEN 12
        WHEN 'CYLINDRICAL' THEN 13
        WHEN 'IRREGULAR_SHAPE' THEN 14
        ELSE 99
    END;

-- Count total order sizes
SELECT COUNT(*) as total_order_sizes FROM order_sizes WHERE status = 'ACTIVE';

-- Check if description column exists
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'order_sizes' 
  AND column_name = 'description';
