-- Add category_type column to categories table
ALTER TABLE categories 
ADD COLUMN category_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Update existing categories based on their names
UPDATE categories 
SET category_type = 'FRAGILE' 
WHERE UPPER(category_name) LIKE '%DỄ VỠ%' 
   OR UPPER(category_name) LIKE '%FRAGILE%'
   OR UPPER(category_name) LIKE '%BREAKABLE%';

-- Set all other categories to NORMAL (already default)
UPDATE categories 
SET category_type = 'NORMAL' 
WHERE category_type IS NULL OR category_type NOT IN ('NORMAL', 'FRAGILE');

-- Add index for better query performance
CREATE INDEX idx_categories_type ON categories(category_type);
