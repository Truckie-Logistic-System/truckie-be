-- Migration: Change stipulation_settings.contents (jsonb) to content (text)
-- Author: System
-- Date: 2025-01-23

-- Rename column and change data type
ALTER TABLE stipulation_settings 
RENAME COLUMN contents TO content;

ALTER TABLE stipulation_settings 
ALTER COLUMN content TYPE TEXT;

-- Add comment for documentation
COMMENT ON COLUMN stipulation_settings.content IS 'HTML content for terms and conditions';
