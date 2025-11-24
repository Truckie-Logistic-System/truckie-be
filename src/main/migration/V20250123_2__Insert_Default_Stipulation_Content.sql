-- Migration: Insert default stipulation content
-- Author: System
-- Date: 2025-01-23

-- Delete existing records first
DELETE FROM stipulation_settings;

-- Insert default stipulation content
INSERT INTO stipulation_settings (id, content, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '<div class="terms-container">
    <h2 style="text-align: center; text-transform: uppercase; margin-bottom: 20px; font-family: ''Times New Roman'', serif;">ĐIỀU KHOẢN VÀ ĐIỀU KIỆN SỬ DỤNG DỊCH VỤ VẬN CHUYỂN</h2>
    
    <hr style="border: 0; border-top: 1px solid #ccc; margin: 20px 0;" />

    <h3>ĐIỀU 1. ĐỊNH NGHĨA VÀ PHẠM VI DỊCH VỤ</h3>
    <p><strong>1.1.</strong> Hệ thống cung cấp dịch vụ vận chuyển hàng hóa bằng xe tải trong khu vực nội thành Thành phố Hồ Chí Minh. Dịch vụ bao gồm việc điều phối phương tiện, tài xế và công nghệ theo dõi đơn hàng theo thời gian thực (GPS).</p>
    <p><strong>1.2.</strong> Phương tiện vận chuyển: Dịch vụ chỉ áp dụng cho phương tiện là xe tải với tổng khối lượng mặt hàng vận chuyển bắt buộc trong khoảng từ <strong>0.5 tấn đến 10 tấn</strong>.</p>
    <p><strong>1.3.</strong> Thời gian giao hàng: Được thực hiện trong ngày theo khung giờ quy định của pháp luật giao thông đường bộ hiện hành.</p>

    <h3>ĐIỀU 2. QUY ĐỊNH VỀ HÀNG HÓA</h3>
    <p><strong>2.1. Hàng hóa được chấp nhận:</strong> Hàng tiêu dùng nhanh (FMCG), hàng điện tử, hàng may mặc, bưu phẩm và các loại hàng hóa không yêu cầu điều kiện bảo quản đặc biệt (như nhiệt độ, độ ẩm).</p>
    <p><strong>2.2. Hàng hóa cấm và hạn chế:</strong></p>
    <p style="padding-left: 20px;">(a) Tuyệt đối không vận chuyển thực phẩm tươi sống, hàng đông lạnh, hàng cấm, chất kích thích, vũ khí hoặc các mặt hàng vi phạm pháp luật Việt Nam.</p>
    <p style="padding-left: 20px;">(b) Mọi trách nhiệm pháp lý liên quan đến tính hợp pháp của hàng hóa thuộc về Khách hàng (Bên A). Công ty không chịu trách nhiệm nếu hàng hóa bị cơ quan chức năng tịch thu do Bên A không khai báo hoặc khai báo sai lệch.</p>
    <p><strong>2.3. Quy đổi trọng lượng:</strong> Đối với hàng hóa dạng lỏng, Bên A có trách nhiệm thực hiện quy đổi sang đơn vị trọng lượng (kg/tấn) trước khi tiến hành đặt hàng trên hệ thống.</p>
    <p><strong>2.4. Đóng gói:</strong> Bên A chịu hoàn toàn trách nhiệm về quy cách đóng gói. Công ty được miễn trừ trách nhiệm đối với các hư hỏng phát sinh do lỗi đóng gói nội bộ của kiện hàng.</p>

    <h3>ĐIỀU 3. QUY TRÌNH ĐẶT HÀNG VÀ KÝ KẾT HỢP ĐỒNG</h3>
    <p><strong>3.1.</strong> Mỗi đơn hàng bắt buộc phải đi kèm một Hợp đồng vận chuyển (Contract). Hợp đồng chỉ có hiệu lực khi được cả hai bên xác nhận ký điện tử trên hệ thống.</p>
    <p><strong>3.2.</strong> Đơn hàng không thể bị hủy nếu quá trình vận chuyển đã bắt đầu (Tài xế đã nhận đơn hoặc đang di chuyển).</p>
    <p><strong>3.3.</strong> Thông tin đơn hàng (địa điểm, người nhận) không được phép thay đổi sau khi đã được phân công cho tài xế.</p>

    <h3>ĐIỀU 4. CHÍNH SÁCH THANH TOÁN VÀ HỦY BỎ</h3>
    <p><strong>4.1. Đặt cọc (Deposit):</strong> Bên A phải thanh toán khoản đặt cọc ngay sau khi ký hợp đồng. Nếu không thực hiện thanh toán cọc trong vòng <strong>24 giờ</strong>, hệ thống sẽ tự động hủy đơn hàng và hợp đồng.</p>
    <p><strong>4.2. Thanh toán hoàn tất:</strong> Khoản tiền còn lại của hợp đồng phải được thanh toán toàn bộ tối thiểu <strong>01 (một) ngày</strong> trước ngày lấy hàng dự kiến. Trường hợp vi phạm, đơn hàng sẽ bị hủy và Công ty không chịu trách nhiệm về sự chậm trễ này.</p>

    <h3>ĐIỀU 5. VẬN HÀNH VÀ NIÊM PHONG (SEAL)</h3>
    <p><strong>5.1. Quy trình kiểm tra:</strong> Trước khi khởi hành, Tài xế sẽ thực hiện kiểm tra tình trạng bên ngoài của hàng hóa và chụp ảnh container/thùng xe đã niêm phong (seal).</p>
    <p><strong>5.2. Niêm phong:</strong> Tài xế không được phép tự ý thay đổi seal nếu không có sự cho phép từ bộ phận điều phối. Mỗi chuyến xe được cấp số lượng seal xác định để phục vụ giám sát.</p>
    <p><strong>5.3. Giám sát hành trình:</strong> Bên A đồng ý rằng vị trí của đơn hàng sẽ được theo dõi liên tục qua GPS với tần suất cập nhật tối thiểu mỗi 60 giây để đảm bảo an toàn và minh bạch. Tài xế không được tự ý thay đổi tuyến đường đã được hệ thống chỉ định.</p>

    <h3>ĐIỀU 6. GIAO NHẬN VÀ XỬ LÝ SỰ CỐ</h3>
    <p><strong>6.1. Nghiệm thu:</strong> Người nhận hàng có trách nhiệm kiểm tra hàng hóa, đối chiếu số lượng/tình trạng và ký xác nhận (ký tên hoặc ký điện tử) vào biên bản giao nhận. Đơn hàng chỉ được xem là hoàn thành khi có ảnh chụp bằng chứng giao hàng và dữ liệu định vị tại điểm giao.</p>
    <p><strong>6.2. Giao hàng thất bại và Trả hàng (Return):</strong></p>
    <p style="padding-left: 20px;">(a) Nếu Người nhận từ chối nhận hàng hoặc không thể liên lạc, quy trình Trả hàng sẽ được kích hoạt.</p>
    <p style="padding-left: 20px;">(b) Bên A (Người gửi) có nghĩa vụ thanh toán cước phí vận chuyển chiều về. Nếu Bên A không thanh toán, Công ty có quyền từ chối vận chuyển hàng về và để lại hàng hóa tại điểm giao; Công ty được miễn trừ mọi trách nhiệm bảo quản đối với hàng hóa đó.</p>
    <p><strong>6.3. Báo cáo sự cố:</strong> Nếu phát hiện hàng hóa bị hỏng hoặc thiếu hụt tại thời điểm giao, Tài xế và Người nhận phải lập biên bản và báo cáo ngay lập tức lên hệ thống để bộ phận vận hành ghi nhận và xử lý đền bù (nếu có).</p>

    <h3>ĐIỀU 7. BẢO MẬT THÔNG TIN</h3>
    <p><strong>7.1.</strong> Mọi quy trình lưu trữ dữ liệu GPS, hình ảnh giao hàng và thông tin cá nhân của các bên đều tuân thủ Luật An ninh mạng và Luật Bảo vệ dữ liệu cá nhân tại Việt Nam.</p>
</div>',
    NOW(),
    NOW()
);
