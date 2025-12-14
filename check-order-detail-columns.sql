-- Script to check if the time fields have been removed from order_detail table
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'order_detail' 
ORDER BY column_name;
