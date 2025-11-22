-- Change distance_kilometers column from INTEGER to DECIMAL(10,2) in journey_segments table
-- This allows storing distance with 2 decimal places (e.g., 15.75 km)

ALTER TABLE journey_segments 
    ALTER COLUMN distance_kilometers TYPE DECIMAL(10, 2);

-- Add comment to document the change
COMMENT ON COLUMN journey_segments.distance_kilometers IS 'Distance in kilometers with 2 decimal places (e.g., 15.75)';
