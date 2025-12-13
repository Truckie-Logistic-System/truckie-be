-- Migration script to synchronize order_details table with OrderDetailEntity
-- Created on 2025-12-14

-- Remove time fields from order_details table if they exist
DO $$
BEGIN
    -- Check if start_time column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'order_details' 
        AND column_name = 'start_time'
    ) THEN
        ALTER TABLE order_details DROP COLUMN start_time;
    END IF;

    -- Check if end_time column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'order_details' 
        AND column_name = 'end_time'
    ) THEN
        ALTER TABLE order_details DROP COLUMN end_time;
    END IF;

    -- Check if estimated_end_time column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'order_details' 
        AND column_name = 'estimated_end_time'
    ) THEN
        ALTER TABLE order_details DROP COLUMN estimated_end_time;
    END IF;
END $$;
