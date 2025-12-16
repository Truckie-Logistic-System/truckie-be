@echo off
chcp 65001
set PGCLIENTENCODING=UTF8
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Kiện hàng nhỏ' WHERE id = '4b74c114-7574-4531-9474-b21382d195db';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Thùng vừa' WHERE id = '7ad7d6d3-5813-4dd0-b5fa-5dcd624a4755';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Nửa pallet' WHERE id = '54b96194-232b-40b0-b5d4-be0c76480240';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Thùng lớn' WHERE id = 'cf360da2-e8e2-4bbd-8d43-b7f366aaf8c3';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Pallet tiêu chuẩn' WHERE id = 'c0891e29-d22b-4d36-9e36-677978f2dd8a';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Pallet lớn' WHERE id = 'cb02ae9b-ece8-47d5-b75d-8df34384e2fe';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Hàng cao' WHERE id = '77c8687e-cecd-42aa-8773-54161bbcce5b';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Hàng dài' WHERE id = '7e9b2076-f4e3-4a20-a1e0-4e5c5b83e552';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Phong bì/Túi nhỏ' WHERE id = 'a1b2c3d4-e5f6-4a5b-8c9d-1e2f3a4b5c6d';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Thùng siêu lớn' WHERE id = 'b2c3d4e5-f6a7-4b5c-9d0e-2f3a4b5c6d7e';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Hàng phẳng' WHERE id = 'c3d4e5f6-a7b8-4c5d-0e1f-3a4b5c6d7e8f';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Hàng hình trụ' WHERE id = 'd4e5f6a7-b8c9-4d5e-1f2a-4b5c6d7e8f9a';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Hàng đặc biệt' WHERE id = 'e5f6a7b8-c9d0-4e5f-2a3b-5c6d7e8f9a0b';"
psql -h 14.225.253.8 -U postgres -d truckie -c "UPDATE order_sizes SET description = 'Pallet đôi' WHERE id = 'f6a7b8c9-d0e1-4f5a-3b4c-6d7e8f9a0b1c';"
echo Done!
