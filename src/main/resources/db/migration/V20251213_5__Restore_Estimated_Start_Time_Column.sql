-- Restore estimated_start_time column to order_details table
-- This column should remain as it's still used in OrderDetailEntity

-- Add back the estimated_start_time column
ALTER TABLE order_details ADD COLUMN estimated_start_time TIMESTAMP;

-- Restore data from archive if available
UPDATE order_details 
SET estimated_start_time = archive.estimated_start_time
FROM order_details_time_archive archive
WHERE order_details.id = archive.id 
AND archive.estimated_start_time IS NOT NULL;
