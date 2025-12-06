-- Update off_route_events table schema for OffRouteEventEntity enhancements
-- This script adds all missing fields for contact confirmation flow and location tracking

-- Step 1: Add new columns for location tracking
ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS previous_distance_from_route_meters DOUBLE PRECISION;

ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS last_location_update_at TIMESTAMP;

-- Step 2: Add new columns for contact confirmation flow
ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS contacted_at TIMESTAMP;

ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS contacted_by UUID;

ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS grace_period_extended BOOLEAN DEFAULT false;

ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS grace_period_extension_count INTEGER DEFAULT 0;

ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS grace_period_extended_at TIMESTAMP;

ALTER TABLE off_route_events 
ADD COLUMN IF NOT EXISTS grace_period_expires_at TIMESTAMP;

-- Step 3: Backfill existing rows with default values
UPDATE off_route_events 
SET grace_period_extended = false 
WHERE grace_period_extended IS NULL;

UPDATE off_route_events 
SET grace_period_extension_count = 0 
WHERE grace_period_extension_count IS NULL;

-- Step 4: Add NOT NULL constraints for critical fields
ALTER TABLE off_route_events 
ALTER COLUMN grace_period_extension_count SET NOT NULL;

-- Step 5: Add comments for documentation
COMMENT ON COLUMN off_route_events.previous_distance_from_route_meters IS 'Previous distance from route for tracking driver return to route';
COMMENT ON COLUMN off_route_events.last_location_update_at IS 'Timestamp of last location update for the event';
COMMENT ON COLUMN off_route_events.contacted_at IS 'When staff contacted the driver about off-route event';
COMMENT ON COLUMN off_route_events.contacted_by IS 'UUID of staff member who contacted the driver';
COMMENT ON COLUMN off_route_events.grace_period_extended IS 'Whether grace period has been extended for this event';
COMMENT ON COLUMN off_route_events.grace_period_extension_count IS 'Number of times grace period has been extended (max 3)';
COMMENT ON COLUMN off_route_events.grace_period_extended_at IS 'When grace period was last extended';
COMMENT ON COLUMN off_route_events.grace_period_expires_at IS 'When the current grace period expires';
