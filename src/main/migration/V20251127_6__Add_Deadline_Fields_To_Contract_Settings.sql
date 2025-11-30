-- Add deadline fields to contract_settings table
ALTER TABLE contract_settings 
ADD COLUMN IF NOT EXISTS deposit_deadline_hours INTEGER DEFAULT 24,
ADD COLUMN IF NOT EXISTS signing_deadline_hours INTEGER DEFAULT 24;

-- Update existing record with default values
UPDATE contract_settings 
SET deposit_deadline_hours = 24,
    signing_deadline_hours = 24
WHERE deposit_deadline_hours IS NULL OR signing_deadline_hours IS NULL;
