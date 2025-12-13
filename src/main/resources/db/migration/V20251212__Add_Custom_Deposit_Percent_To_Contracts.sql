-- Add custom deposit percentage column to contracts table
-- This allows per-contract deposit percentage override (NULL means use global setting from contract_settings)

ALTER TABLE contracts 
ADD COLUMN IF NOT EXISTS custom_deposit_percent DECIMAL(5,2) DEFAULT NULL;

-- Add comment for documentation
COMMENT ON COLUMN contracts.custom_deposit_percent IS 'Custom deposit percentage for this contract (0-100). NULL means use global setting from contract_settings table.';
