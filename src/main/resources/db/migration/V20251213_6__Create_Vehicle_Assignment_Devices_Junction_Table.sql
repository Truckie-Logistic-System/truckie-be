-- Migration to create junction table for many-to-many relationship between vehicle_assignments and devices
-- Also remove the old device_ids column from vehicle_assignments

-- Create junction table
CREATE TABLE vehicle_assignment_devices (
    vehicle_assignment_id UUID NOT NULL,
    device_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (vehicle_assignment_id, device_id),
    CONSTRAINT fk_vehicle_assignment_devices_va 
        FOREIGN KEY (vehicle_assignment_id) REFERENCES vehicle_assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_assignment_devices_device 
        FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_vehicle_assignment_devices_va_id ON vehicle_assignment_devices(vehicle_assignment_id);
CREATE INDEX idx_vehicle_assignment_devices_device_id ON vehicle_assignment_devices(device_id);

-- Migrate existing data from device_ids column if it exists and has data
-- This will parse the comma-separated device IDs and create junction records
DO $$
DECLARE
    va_record RECORD;
    device_id_text TEXT;
    device_uuid UUID;
BEGIN
    -- Check if device_ids column exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'vehicle_assignments' 
        AND column_name = 'device_ids'
    ) THEN
        -- Loop through all vehicle assignments that have device_ids
        FOR va_record IN 
            SELECT id, device_ids 
            FROM vehicle_assignments 
            WHERE device_ids IS NOT NULL AND device_ids != ''
        LOOP
            -- Split comma-separated device IDs and insert into junction table
            DECLARE
                device_ids_array TEXT[];
                i INTEGER;
            BEGIN
                -- Split the comma-separated string
                device_ids_array := string_to_array(va_record.device_ids, ',');
                
                -- Loop through each device ID
                FOR i IN 1..array_length(device_ids_array, 1)
                LOOP
                    device_id_text := trim(device_ids_array[i]);
                    
                    -- Try to convert to UUID and check if device exists
                    BEGIN
                        device_uuid := device_id_text::UUID;
                        
                        -- Only insert if device exists
                        IF EXISTS (SELECT 1 FROM devices WHERE id = device_uuid) THEN
                            INSERT INTO vehicle_assignment_devices (vehicle_assignment_id, device_id)
                            VALUES (va_record.id, device_uuid)
                            ON CONFLICT (vehicle_assignment_id, device_id) DO NOTHING;
                        END IF;
                    EXCEPTION 
                        WHEN invalid_text_representation THEN
                            -- Skip invalid UUIDs
                            CONTINUE;
                    END;
                END LOOP;
            END;
        END LOOP;
        
        -- Drop the old device_ids column
        ALTER TABLE vehicle_assignments DROP COLUMN device_ids;
        
        RAISE NOTICE 'Migrated device assignments data and removed device_ids column';
    END IF;
END $$;
