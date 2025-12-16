-- Check current order_sizes data with proper column names
SELECT 
    id,
    min_height,
    max_height,
    min_length,
    max_length,
    min_width,
    max_width,
    description,
    status,
    created_at
FROM order_sizes
WHERE status = 'ACTIVE'
ORDER BY 
    CASE 
        WHEN max_length <= 0.30 AND max_width <= 0.20 THEN 1  -- MINI_PARCEL
        WHEN max_length <= 0.50 AND max_width <= 0.40 THEN 2  -- SMALL_PARCEL
        WHEN max_length <= 0.90 AND max_width <= 0.70 THEN 3  -- MEDIUM_BOX
        WHEN max_length <= 1.20 AND max_width <= 1.00 THEN 4  -- LARGE_BOX
        WHEN max_length <= 1.50 AND max_width <= 1.50 THEN 5  -- EXTRA_LARGE_BOX
        WHEN max_length <= 1.20 AND max_width <= 0.80 THEN 6  -- HALF_PALLET
        WHEN max_length <= 1.20 AND max_width <= 1.00 THEN 7  -- STANDARD_PALLET
        WHEN max_length <= 1.20 AND max_width <= 1.20 THEN 8  -- LARGE_PALLET
        WHEN max_length > 2.00 THEN 10  -- LONG_ITEM or DOUBLE_PALLET
        WHEN max_height > 2.00 THEN 11  -- TALL_ITEM
        ELSE 99
    END,
    max_length DESC;

-- Count total order sizes
SELECT COUNT(*) as total_active_order_sizes FROM order_sizes WHERE status = 'ACTIVE';

-- Check for description column
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'Description column exists'
        ELSE 'Description column missing'
    END as description_status
FROM information_schema.columns
WHERE table_name = 'order_sizes' 
  AND column_name = 'description';
