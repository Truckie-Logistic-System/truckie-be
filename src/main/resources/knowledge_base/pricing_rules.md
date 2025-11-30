# QUY TẮC TÍNH GIÁ VẬN CHUYỂN

## ⚠️ Phạm Vi Dịch Vụ

**CHỈ VẬN CHUYỂN BẰNG XE TẢI TRONG NỘI THÀNH TP. HỒ CHÍ MINH**

- ✅ Phương tiện: XE TẢI duy nhất
- ❌ KHÔNG hỗ trợ: Xe máy, xe ba gác, container đầu kéo
- ✅ Tải trọng: 0.5 tấn đến 10 tấn
- ✅ Khu vực: Nội thành TP.HCM (liên tỉnh cần liên hệ riêng)

## Công Thức Tính Giá

**Giá = (Base Price × Distance) × Category Multiplier + Category Extra Fee**

### Các Bước Tính:

1. **Chọn loại xe phù hợp** dựa trên trọng lượng hàng
2. **Áp dụng Distance Tiers** (giá theo từng khoảng cách):
   - **0-4 km**: Giá cố định (ví dụ: 50,000-200,000 VND tùy xe)
   - **4-20 km**: X VND/km (thấp hơn)
   - **20-50 km**: Y VND/km (trung bình)
   - **50+ km**: Z VND/km (cao nhất)
3. **Nhân với số xe** (nếu cần nhiều xe do hàng quá nặng)
4. **Nhân với hệ số loại hàng** (category multiplier)
5. **Cộng phụ phí loại hàng** (category extra fee)

### Loại Xe Theo Trọng Lượng (8 loại)

- **TRUCK_600**: Xe tải nhỏ, phù hợp cho hàng hóa từ 0 đến 600 kg (thường dùng giao hàng nội thành, quãng đường ngắn)
- **TRUCK_1.25_TON**: Xe tải 1.25 tấn (chở hàng vừa và nhỏ, vận chuyển nội thành)
- **TRUCK_1.9_TON**: Xe tải 1.9 tấn (phù hợp cho hàng hóa nặng hơn, khoảng 1 tấn đến 1.5 tấn)
- **TRUCK_2.4_TONN**: Xe tải 2.4 tấn (phù hợp hàng hóa nặng hơn, từ 1.5 tấn đến 2 tấn)
- **TRUCK_3.5_TON**: Xe tải 3.5 tấn (dùng nhiều trong logistics, chuyển hàng hóa trọng lượng trung bình)
- **TRUCK_5_TON**: Xe tải 5 tấn (phù hợp vận chuyển số lượng lớn)
- **TRUCK_7_TON**: Xe tải 7 tấn (thường dùng trong khu công nghiệp, chở hàng nặng)
- **TRUCK_10_TON**: Xe tải lớn nhất của chúng tôi, có thể chở từ 5.000 kg đến 10.000 kg (chuyên dùng cho hàng hóa di tĩnh)

Hệ thống tự động chọn xe nhỏ nhất đủ chở để tối ưu chi phí cho khách hàng.

### Loại Hàng Hóa (Category)

- **Hàng thông thường**: 
  - Hệ số: 1.0x
  - Phụ phí: 0 VND
  - Ví dụ: quần áo, thực phẩm đóng gói, văn phòng phẩm

- **Hàng dễ vỡ**: 
  - Hệ số: 1.2x
  - Phụ phí: 20,000 VND
  - Ví dụ: đồ gốm sứ, thủy tinh, điện tử

- **Hàng nguy hiểm**: 
  - Hệ số: 1.5x
  - Phụ phí: 50,000 VND
  - Ví dụ: hóa chất, chất lỏng dễ cháy (có giấy phép)

⚠️ **LƯU Ý**: Thực phẩm tươi sống, hàng đông lạnh, động vật sống **KHÔNG ĐƯỢC VẬN CHUYỂN** (Xem FAQ - Hàng cấm)

### Ví Dụ Tính Phí

**Ví dụ 1: Đơn hàng 5 tấn gạo, 100 km, hàng thông thường**

1. Chọn xe: **TRUCK_5_TON** (xe tải 5 tấn)
2. Tính theo distance tiers:
   - 0-4 km: 100,000 VND (fixed)
   - 4-20 km: 16 km × 8,000 = 128,000 VND
   - 20-50 km: 30 km × 6,000 = 180,000 VND
   - 50-100 km: 50 km × 5,000 = 250,000 VND
3. Tổng cơ bản: 658,000 VND
4. Category (thông thường): 658,000 × 1.0 + 0 = 658,000 VND
5. **Giá ước tính: ~660,000 VND**

**Ví dụ 2: Đơn hàng 12 tấn xi măng, 50 km, hàng thông thường**

1. Chọn xe: **TRUCK_10_TON** (xe tải 10 tấn - xe lớn nhất)
2. ⚠️ **Cần 2 xe** vì hàng 12 tấn > xe 10 tấn
3. Tính giá 1 xe (50 km):
   - 0-4 km: 120,000 VND
   - 4-20 km: 16 km × 10,000 = 160,000 VND
   - 20-50 km: 30 km × 8,000 = 240,000 VND
   - Tổng 1 xe: 520,000 VND
4. **Tổng cho 2 xe: 520,000 × 2 = 1,040,000 VND**

---

## 🛡️ Phí Bảo Hiểm Hàng Hóa (Tùy Chọn)

### Công Thức Tính Phí Bảo Hiểm

**Phí BH = Giá trị Khai báo × Tỷ lệ BH × (1 + 10% VAT)**

### Tỷ Lệ Phí Bảo Hiểm

| Loại Hàng | Tỷ lệ (chưa VAT) | Tỷ lệ (có VAT) | Ví dụ (100 triệu VNĐ) |
|-----------|------------------|----------------|----------------------|
| **Hàng thông thường** | 0.08% | 0.088% | 88.000 VNĐ |
| **Hàng dễ vỡ/Rủi ro cao** | 0.15% | 0.165% | 165.000 VNĐ |

### Ví Dụ Tính Phí Bảo Hiểm

**Đơn hàng: 3 kiện hàng điện tử (dễ vỡ), tổng giá trị khai báo 200.000.000 VNĐ**

1. Loại hàng: **Dễ vỡ** → Tỷ lệ 0.15% (chưa VAT)
2. Phí BH (chưa VAT) = 200.000.000 × 0.15% = **300.000 VNĐ**
3. VAT = 300.000 × 10% = **30.000 VNĐ**
4. **Tổng phí BH = 330.000 VNĐ** (hoặc 200.000.000 × 0.165%)

### Quyền Lợi Khi Mua Bảo Hiểm

✅ **CÓ MUA BẢO HIỂM + CÓ CHỨNG TỪ**:
- Bồi thường = Tỷ lệ hư hại × Giá trị Khai báo
- **Ví dụ**: Hàng 100 triệu, hư 50% → Bồi thường **50 triệu VNĐ**

❌ **KHÔNG MUA BẢO HIỂM** (hoặc không có chứng từ):
- Bồi thường tối đa = **10 × Cước phí vận chuyển**
- **Ví dụ**: Cước 500.000 VNĐ → Bồi thường tối đa **5 triệu VNĐ** (dù hàng 100 triệu)

⚠️ **QUAN TRỌNG**: Khách hàng **BẮT BUỘC** cung cấp **Hóa đơn VAT, chứng từ mua bán** chứng minh giá trị hàng khi xảy ra sự cố. Nếu không → Bảo hiểm bị **VÔ HIỆU HÓA**.

---

### Lưu Ý Quan Trọng

⚠️ **Giá tham khảo**: Đây chỉ là ước tính dựa trên công thức chuẩn. Giá thực tế có thể thay đổi tùy theo:

- **Điều kiện đường xá**: Đường đèo, đường xấu, khu vực khó đi
- **Thời gian giao hàng**: Giờ cao điểm (+10-20%), đêm khuya (+20-30%)
- **Khu vực**: Vùng sâu vùng xa, khu công nghiệp, cảng biển
- **Phụ phí bổ sung**: Cầu đường, phà, cao tốc, bốc xếp
- **Bảo hiểm**: Phí BH cộng thêm nếu khách chọn mua (0.088% - 0.165% giá trị khai báo)
- **Khuyến mãi**: Hợp đồng dài hạn, khách hàng VIP có thể được giảm giá

📞 **Liên hệ nhân viên** để nhận báo giá chính xác sau khi đo đạc thực tế và khảo sát tuyến đường.
