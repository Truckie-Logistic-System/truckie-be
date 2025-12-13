-- Migration script to remove unused time fields from order_detail table
-- Created on 2025-12-13

-- First, check if the columns exist before trying to drop them
DO $$
BEGIN
    -- Check if start_time column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'order_detail' 
        AND column_name = 'start_time'
    ) THEN
        ALTER TABLE order_detail DROP COLUMN start_time;
    END IF;

    -- Check if end_time column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'order_detail' 
        AND column_name = 'end_time'
    ) THEN
        ALTER TABLE order_detail DROP COLUMN end_time;
    END IF;

    -- Check if estimated_end_time column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'order_detail' 
        AND column_name = 'estimated_end_time'
    ) THEN
        ALTER TABLE order_detail DROP COLUMN estimated_end_time;
    END IF;
END $$;

-- Update any views or functions that might reference these columns
-- (Add specific view/function updates here if needed)

-- Liquibase will automatically log this change in the databasechangelog table
