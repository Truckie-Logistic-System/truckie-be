-- Migration: Insert Category Pricing Details for AI Chatbot
-- Date: 2025-01-23
-- Purpose: Add pricing multipliers and extra fees for different cargo categories

-- Insert sample categories if not exist (idempotent)
INSERT INTO categories (id, category_name, description, created_at, updated_at, is_deleted)
VALUES 
    (gen_random_uuid(), 'Hàng thông thường', 'Hàng hóa thông thường không cần xử lý đặc biệt', NOW(), NOW(), false),
    (gen_random_uuid(), 'Hàng dễ vỡ', 'Hàng dễ vỡ như đồ gốm sứ, thủy tinh, điện tử', NOW(), NOW(), false),
    (gen_random_uuid(), 'Hàng nguy hiểm', 'Hóa chất, chất lỏng dễ cháy (có giấy phép)', NOW(), NOW(), false)
ON CONFLICT DO NOTHING;

-- Insert category pricing details
-- Note: Using subquery to get category IDs dynamically

-- 1. Hàng thông thường: 1.0x multiplier, 0 VND extra fee
INSERT INTO category_pricing_detail (id, category_id, price_multiplier, extra_fee, created_at, updated_at, is_deleted)
SELECT 
    gen_random_uuid(),
    c.id,
    1.0,
    0,
    NOW(),
    NOW(),
    false
FROM categories c
WHERE c.category_name = 'Hàng thông thường'
  AND NOT EXISTS (
      SELECT 1 FROM category_pricing_detail cpd 
      WHERE cpd.category_id = c.id
  );

-- 2. Hàng dễ vỡ: 1.2x multiplier, 20,000 VND extra fee
INSERT INTO category_pricing_detail (id, category_id, price_multiplier, extra_fee, created_at, updated_at, is_deleted)
SELECT 
    gen_random_uuid(),
    c.id,
    1.2,
    20000,
    NOW(),
    NOW(),
    false
FROM categories c
WHERE c.category_name = 'Hàng dễ vỡ'
  AND NOT EXISTS (
      SELECT 1 FROM category_pricing_detail cpd 
      WHERE cpd.category_id = c.id
  );

-- 3. Hàng nguy hiểm: 1.5x multiplier, 50,000 VND extra fee
INSERT INTO category_pricing_detail (id, category_id, price_multiplier, extra_fee, created_at, updated_at, is_deleted)
SELECT 
    gen_random_uuid(),
    c.id,
    1.5,
    50000,
    NOW(),
    NOW(),
    false
FROM categories c
WHERE c.category_name = 'Hàng nguy hiểm'
  AND NOT EXISTS (
      SELECT 1 FROM category_pricing_detail cpd 
      WHERE cpd.category_id = c.id
  );

-- Verify insertion
DO $$
DECLARE
    count_categories INTEGER;
    count_pricing_details INTEGER;
BEGIN
    SELECT COUNT(*) INTO count_categories FROM categories WHERE category_name IN ('Hàng thông thường', 'Hàng dễ vỡ', 'Hàng nguy hiểm');
    SELECT COUNT(*) INTO count_pricing_details FROM category_pricing_detail;
    
    RAISE NOTICE '✅ Inserted % categories', count_categories;
    RAISE NOTICE '✅ Inserted % category pricing details', count_pricing_details;
END $$;
