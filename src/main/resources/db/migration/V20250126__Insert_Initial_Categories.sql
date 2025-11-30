-- Migration: Insert Initial Categories from CategoryEnum
-- Date: 2025-01-26
-- Purpose: Seed database with categories that were previously hardcoded as enum values

-- Insert initial categories if not exist (idempotent)
INSERT INTO categories (id, category_name, description, created_at, updated_at, created_by, modified_by)
VALUES 
    (gen_random_uuid(), 'NORMAL', 'Hàng hóa thông thường', NOW(), NOW(), 'system', 'system'),
    (gen_random_uuid(), 'FRAGILE', 'Hàng hóa dễ vỡ, cần xử lý cẩn thận', NOW(), NOW(), 'system', 'system'),
    (gen_random_uuid(), 'BULKY', 'Hàng hóa cồng kềnh, kích thước lớn', NOW(), NOW(), 'system', 'system'),
    (gen_random_uuid(), 'DANGEROUS', 'Hàng hóa nguy hiểm, cần biện pháp an toàn đặc biệt', NOW(), NOW(), 'system', 'system')
ON CONFLICT (category_name) DO NOTHING;

-- Add unique constraint to prevent duplicate category names (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_category_name' 
        AND conrelid = 'categories'::regclass
    ) THEN
        ALTER TABLE categories ADD CONSTRAINT uk_category_name UNIQUE (category_name);
    END IF;
END $$;

-- Verify insertion
DO $$
DECLARE
    count_categories INTEGER;
BEGIN
    SELECT COUNT(*) INTO count_categories FROM categories WHERE category_name IN ('NORMAL', 'FRAGILE', 'BULKY', 'DANGEROUS');
    
    RAISE NOTICE '✅ Inserted % initial categories from enum', count_categories;
END $$;
