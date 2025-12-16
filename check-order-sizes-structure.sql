-- Check order_sizes table structure
SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'order_sizes'
ORDER BY ordinal_position;

-- Check current data
SELECT * FROM order_sizes LIMIT 10;
