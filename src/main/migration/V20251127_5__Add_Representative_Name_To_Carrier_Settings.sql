-- Add representative_name column to carrier_settings table
ALTER TABLE carrier_settings 
ADD COLUMN IF NOT EXISTS representative_name VARCHAR(100);

-- Update existing record with default representative name
UPDATE carrier_settings 
SET representative_name = 'Nguyễn Văn A'
WHERE representative_name IS NULL;
