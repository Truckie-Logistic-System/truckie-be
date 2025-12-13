-- Migration: Sync vehicle_service_record table with VehicleServiceRecordEntity
-- This migration standardizes column names and removes unused columns

-- Step 1: Rename columns to match entity field names
-- Rename status → service_type (stores service type string)
ALTER TABLE vehicle_service_record RENAME COLUMN status TO service_type;

-- Rename maintenance_date → planned_date
ALTER TABLE vehicle_service_record RENAME COLUMN maintenance_date TO planned_date;

-- Rename completed_at → actual_date
ALTER TABLE vehicle_service_record RENAME COLUMN completed_at TO actual_date;

-- Step 2: Drop unused columns (these are legacy columns not used in the new entity)
-- Note: is_demo_data is kept as it's part of BaseEntity
ALTER TABLE vehicle_service_record DROP COLUMN IF EXISTS next_maintenance_date;
ALTER TABLE vehicle_service_record DROP COLUMN IF EXISTS failed_at;
ALTER TABLE vehicle_service_record DROP COLUMN IF EXISTS failure_reason;
ALTER TABLE vehicle_service_record DROP COLUMN IF EXISTS request_at;
ALTER TABLE vehicle_service_record DROP COLUMN IF EXISTS started_at;

-- Step 3: Ensure correct column types and constraints
-- service_type should be VARCHAR(100)
ALTER TABLE vehicle_service_record ALTER COLUMN service_type TYPE VARCHAR(100);

-- Step 4: Add comments for documentation
COMMENT ON TABLE vehicle_service_record IS 'Vehicle service records for maintenance and inspections';
COMMENT ON COLUMN vehicle_service_record.service_type IS 'Service type name from config (e.g., Đăng kiểm định kỳ, Bảo dưỡng định kỳ)';
COMMENT ON COLUMN vehicle_service_record.service_status IS 'Record status: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN vehicle_service_record.planned_date IS 'Planned/scheduled date for the service';
COMMENT ON COLUMN vehicle_service_record.actual_date IS 'Actual completion date';
COMMENT ON COLUMN vehicle_service_record.next_service_date IS 'Next scheduled service date (set when completed)';
COMMENT ON COLUMN vehicle_service_record.description IS 'Service description';
COMMENT ON COLUMN vehicle_service_record.odometer_reading IS 'Odometer reading at service time (km)';
COMMENT ON COLUMN vehicle_service_record.vehicle_id IS 'Reference to vehicle';
