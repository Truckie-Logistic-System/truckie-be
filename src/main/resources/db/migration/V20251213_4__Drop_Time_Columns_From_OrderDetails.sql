-- Drop time-related columns from order_details table
-- These fields were removed from OrderDetailEntity

-- Archive data before dropping (safety measure)
CREATE TABLE IF NOT EXISTS order_details_time_archive AS 
SELECT 
    id,
    start_time,
    end_time,
    estimated_end_time,
    estimated_start_time,
    NOW() as archived_at
FROM order_details 
WHERE start_time IS NOT NULL 
   OR end_time IS NOT NULL 
   OR estimated_end_time IS NOT NULL 
   OR estimated_start_time IS NOT NULL;

-- Drop the time columns that are no longer in the entity
ALTER TABLE order_details DROP COLUMN IF EXISTS start_time;
ALTER TABLE order_details DROP COLUMN IF EXISTS end_time;
ALTER TABLE order_details DROP COLUMN IF EXISTS estimated_end_time;
ALTER TABLE order_details DROP COLUMN IF EXISTS estimated_start_time;
