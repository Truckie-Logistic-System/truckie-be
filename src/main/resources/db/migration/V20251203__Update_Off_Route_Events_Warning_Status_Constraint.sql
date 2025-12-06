-- Update warning_status CHECK constraint to include all valid statuses
-- This allows the off-route event flow to properly handle all states

-- Step 1: Drop the existing constraint
ALTER TABLE off_route_events DROP CONSTRAINT IF EXISTS off_route_events_warning_status_check;

-- Step 2: Add the updated constraint with all valid statuses
ALTER TABLE off_route_events 
ADD CONSTRAINT off_route_events_warning_status_check 
CHECK (warning_status IN (
    'NONE',
    'YELLOW_SENT',
    'RED_SENT', 
    'CONTACTED_WAITING_RETURN',
    'CONTACT_FAILED',
    'RESOLVED_SAFE',
    'BACK_ON_ROUTE',
    'ISSUE_CREATED'
));

-- Step 3: Add comment explaining the constraint
COMMENT ON CONSTRAINT off_route_events_warning_status_check ON off_route_events 
IS 'Ensures warning_status is one of the valid enum values for off-route events';
