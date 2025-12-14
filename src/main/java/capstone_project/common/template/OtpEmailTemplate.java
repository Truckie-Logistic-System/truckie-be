package capstone_project.common.template;

public class OtpEmailTemplate {

    public static String getOtpEmailTemplate() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <style>\n" +
                "        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }\n" +
                "        .container { max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); overflow: hidden; }\n" +
                "        .header { background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%); color: white; padding: 25px; text-align: center; }\n" +
                "        .header h1 { margin: 0; font-size: 28px; font-weight: bold; }\n" +
                "        .content { padding: 30px; }\n" +
                "        .otp { font-size: 32px; font-weight: bold; color: #1e40af; text-align: center; margin: 25px 0; padding: 15px; background-color: #f0f7ff; border-radius: 8px; letter-spacing: 5px; }\n" +
                "        .section-title { font-size: 20px; color: #1e40af; margin-bottom: 15px; padding-bottom: 10px; font-weight: bold; border-bottom: 1px solid #e5e7eb; }\n" +
                "        .footer { background-color: #f1f5f9; padding: 20px; text-align: center; font-size: 14px; color: #64748b; }\n" +
                "        h2 { color: #1e40af; font-size: 22px; margin-top: 0; margin-bottom: 15px; font-weight: bold; }\n" +
                "        ul { padding-left: 20px; margin-bottom: 20px; }\n" +
                "        ul li { margin-bottom: 10px; color: #374151; }\n" +
                "        a { color: #3b82f6; text-decoration: none; font-weight: bold; }\n" +
                "        a:hover { text-decoration: underline; }\n" +
                "        .note { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px; margin: 20px 0; color: #92400e; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>Truckie</h1>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Xin chào!</h2>\n" +
                "            <p>Cảm ơn bạn đã đăng ký tài khoản doanh nghiệp trên hệ thống <strong>Truckie</strong>. Để hoàn tất đăng ký, vui lòng sử dụng mã OTP dưới đây:</p>\n" +
                "            <p class=\"otp\">%s</p>\n" +
                "            <p class=\"note\"><strong>Lưu ý:</strong> Mã OTP này chỉ có hiệu lực trong vòng <strong>5 phút</strong>. Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này hoặc liên hệ với đội ngũ hỗ trợ của chúng tôi.</p>\n" +
                "            <p>Sau khi xác thực OTP thành công, tài khoản của bạn sẽ được chuyển đến quản trị viên để kích hoạt. Chúng tôi sẽ thông báo cho bạn khi tài khoản được kích hoạt.</p>\n" +
                "            <h2 class=\"section-title\">Thông tin liên hệ</h2>\n" +
                "            <p><strong>Nếu bạn có bất kỳ câu hỏi hoặc cần hỗ trợ, vui lòng liên hệ:</strong></p>\n" +
                "            <ul>\n" +
                "                <li><strong>Điện thoại:</strong> 0123 456 789</li>\n" +
                "                <li><strong>Email:</strong> <a href=\"mailto:support@truckie.vn\">support@truckie.vn</a></li>\n" +
                "                <li><strong>Website:</strong> <a href=\"https://www.truckie.vn\">www.truckie.vn</a></li>\n" +
                "            </ul>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>&copy; 2025 Truckie - Hệ thống quản lý vận tải. Đây là email tự động, vui lòng không trả lời.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}