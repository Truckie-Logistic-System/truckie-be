-- Migration: Cleanup maintenance_types table and sync database with code
-- This migration removes the old maintenance_types table if it exists
-- Service types are now managed via backend config (list-config.properties)

-- Drop maintenance_types table if exists (no longer needed)
DROP TABLE IF EXISTS maintenance_types CASCADE;

-- Drop any foreign key constraints referencing maintenance_types
-- (vehicle_service_record no longer has maintenance_type_id)
ALTER TABLE vehicle_service_record DROP COLUMN IF EXISTS maintenance_type_id;

-- Ensure vehicle_service_record has all required columns
-- These columns should already exist from previous migrations, but add IF NOT EXISTS for safety

-- Add service_status column if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'vehicle_service_record' 
                   AND column_name = 'service_status') THEN
        ALTER TABLE vehicle_service_record ADD COLUMN service_status VARCHAR(20);
    END IF;
END $$;

-- Add next_service_date column if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'vehicle_service_record' 
                   AND column_name = 'next_service_date') THEN
        ALTER TABLE vehicle_service_record ADD COLUMN next_service_date TIMESTAMP;
    END IF;
END $$;

-- Add comments to clarify the new structure
COMMENT ON TABLE vehicle_service_record IS 'Vehicle service records - service types are now strings from backend config';
COMMENT ON COLUMN vehicle_service_record.status IS 'Service type name (e.g., Đăng kiểm định kỳ, Bảo dưỡng định kỳ)';
COMMENT ON COLUMN vehicle_service_record.service_status IS 'Record status: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED';
COMMENT ON COLUMN vehicle_service_record.maintenance_date IS 'Planned date for the service';
COMMENT ON COLUMN vehicle_service_record.completed_at IS 'Actual completion date';
COMMENT ON COLUMN vehicle_service_record.next_service_date IS 'Next scheduled service date';
