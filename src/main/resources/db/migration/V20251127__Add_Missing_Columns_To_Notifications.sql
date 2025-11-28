-- Add missing columns from BaseEntity to notifications table
-- These columns are required by BaseEntity but were missing in the original migration

-- Add created_by column (nullable since existing records won't have this)
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

-- Add modified_by column
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS modified_by VARCHAR(255);

-- Add modified_at column (BaseEntity uses modified_at instead of updated_at)
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS modified_at TIMESTAMP;

-- Comment for documentation
COMMENT ON COLUMN notifications.created_by IS 'User who created the notification (from BaseEntity auditing)';
COMMENT ON COLUMN notifications.modified_by IS 'User who last modified the notification (from BaseEntity auditing)';
COMMENT ON COLUMN notifications.modified_at IS 'Timestamp of last modification (from BaseEntity auditing)';
