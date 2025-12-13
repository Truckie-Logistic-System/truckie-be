-- Rename vehicle_maintenance table to vehicle_service_record
ALTER TABLE vehicle_maintenance RENAME TO vehicle_service_record;

-- Drop the expire_maintenance_date column as we no longer track expiry dates
ALTER TABLE vehicle_service_record DROP COLUMN IF EXISTS expire_maintenance_date;

-- Add next_service_date column for tracking next maintenance/inspection date
ALTER TABLE vehicle_service_record ADD COLUMN IF NOT EXISTS next_service_date TIMESTAMP;

-- Add comment to clarify the purpose of the table
COMMENT ON TABLE vehicle_service_record IS 'Stores vehicle service records including maintenance and inspection schedules';
COMMENT ON COLUMN vehicle_service_record.maintenance_date IS 'Planned date for maintenance/inspection';
COMMENT ON COLUMN vehicle_service_record.completed_at IS 'Actual date when maintenance/inspection was completed';
COMMENT ON COLUMN vehicle_service_record.next_service_date IS 'Next scheduled date for this type of service';
COMMENT ON COLUMN vehicle_service_record.service_status IS 'Status: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED';
