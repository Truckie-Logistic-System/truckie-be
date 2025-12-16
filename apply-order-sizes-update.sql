-- Update existing order sizes with Vietnamese descriptions
UPDATE order_sizes SET description = 'Kiện hàng nhỏ - Phù hợp cho tài liệu, phụ kiện, hàng nhỏ gọn (tối đa 0.5m × 0.4m × 0.4m)' WHERE id = '4b74c114-7574-4531-9474-b21382d195db';
UPDATE order_sizes SET description = 'Thùng vừa - Phù hợp cho quần áo, giày dép, đồ gia dụng nhỏ (tối đa 0.9m × 0.7m × 0.7m)' WHERE id = '7ad7d6d3-5813-4dd0-b5fa-5dcd624a4755';
UPDATE order_sizes SET description = 'Nửa pallet - Phù hợp cho hàng công nghiệp nhỏ, thùng carton nhiều (tối đa 0.8m × 0.6m × 1.5m)' WHERE id = '54b96194-232b-40b0-b5d4-be0c76480240';
UPDATE order_sizes SET description = 'Thùng lớn - Phù hợp cho đồ điện tử, đồ nội thất nhỏ (tối đa 1.2m × 1.0m × 1.2m)' WHERE id = 'cf360da2-e8e2-4bbd-8d43-b7f366aaf8c3';
UPDATE order_sizes SET description = 'Pallet tiêu chuẩn - Phù hợp cho hàng công nghiệp, hàng xuất khẩu (tối đa 1.2m × 0.8m × 1.8m)' WHERE id = 'c0891e29-d22b-4d36-9e36-677978f2dd8a';
UPDATE order_sizes SET description = 'Pallet lớn - Phù hợp cho hàng công nghiệp lớn, máy móc (tối đa 1.2m × 1.0m × 1.8m)' WHERE id = 'cb02ae9b-ece8-47d5-b75d-8df34384e2fe';
UPDATE order_sizes SET description = 'Hàng cao - Phù hợp cho tủ lạnh, tủ đứng, cột trụ (tối đa 1.2m × 1.2m × 2.2m)' WHERE id = '77c8687e-cecd-42aa-8773-54161bbcce5b';
UPDATE order_sizes SET description = 'Hàng dài - Phù hợp cho ống nhựa, sắt thép, thanh gỗ (tối đa 2.5m × 0.5m × 0.5m)' WHERE id = '7e9b2076-f4e3-4a20-a1e0-4e5c5b83e552';

-- Insert new order sizes
INSERT INTO order_sizes (id, min_height, max_height, min_length, max_length, min_width, max_width, description, status, created_at, modified_at)
VALUES 
-- MINI_PARCEL
('a1b2c3d4-e5f6-4a5b-8c9d-1e2f3a4b5c6d', 0.01, 0.05, 0.10, 0.30, 0.10, 0.20, 'Phong bì/Túi nhỏ - Phù hợp cho tài liệu, thư từ, hàng siêu nhỏ (tối đa 0.3m × 0.2m × 0.05m)', 'ACTIVE', NOW(), NOW()),

-- EXTRA_LARGE_BOX
('b2c3d4e5-f6a7-4b5c-9d0e-2f3a4b5c6d7e', 1.21, 1.50, 1.21, 1.50, 1.21, 1.50, 'Thùng siêu lớn - Phù hợp cho tủ lạnh, máy giặt, nội thất lớn (tối đa 1.5m × 1.5m × 1.5m)', 'ACTIVE', NOW(), NOW()),

-- FLAT_ITEM
('c3d4e5f6-a7b8-4c5d-0e1f-3a4b5c6d7e8f', 0.05, 0.15, 2.00, 2.50, 1.20, 1.50, 'Hàng phẳng - Phù hợp cho tấm gỗ, tấm kính, cửa, bảng quảng cáo (tối đa 2.5m × 1.5m × 0.15m)', 'ACTIVE', NOW(), NOW()),

-- CYLINDRICAL
('d4e5f6a7-b8c9-4d5e-1f2a-4b5c6d7e8f9a', 0.20, 0.30, 1.50, 2.00, 0.20, 0.30, 'Hàng hình trụ - Phù hợp cho cuộn vải, cuộn thảm, ống nhựa tròn (đường kính 0.3m × dài 2.0m)', 'ACTIVE', NOW(), NOW()),

-- IRREGULAR_SHAPE
('e5f6a7b8-c9d0-4e5f-2a3b-5c6d7e8f9a0b', 0.80, 1.50, 0.80, 1.00, 0.80, 1.00, 'Hàng hình dạng đặc biệt - Phù hợp cho xe đạp, xe máy điện, máy móc (bounding box 1.0m × 1.0m × 1.5m)', 'ACTIVE', NOW(), NOW()),

-- DOUBLE_PALLET
('f6a7b8c9-d0e1-4f5a-3b4c-6d7e8f9a0b1c', 0.90, 1.00, 2.00, 2.40, 1.10, 1.20, 'Pallet đôi - Phù hợp cho hàng công nghiệp lớn, hàng xuất khẩu trên 2 pallet ghép (tối đa 2.4m × 1.2m × 1.0m)', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Verify results
SELECT COUNT(*) as total_order_sizes FROM order_sizes WHERE status = 'ACTIVE';
