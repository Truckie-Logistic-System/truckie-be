package capstone_project.common.template;

public class OtpEmailForgetPasswordTemplate {
    public static String getOtpEmailForgetPasswordTemplate() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <style>\n" +
                "        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f7fa; margin: 0; padding: 20px; }\n" +
                "        .container { max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); overflow: hidden; }\n" +
                "        .header { background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%); color: white; padding: 30px; text-align: center; }\n" +
                "        .header h1 { margin: 0; font-size: 24px; font-weight: 600; }\n" +
                "        .header .logo { font-size: 32px; font-weight: bold; margin-bottom: 10px; }\n" +
                "        .content { padding: 30px; color: #333; }\n" +
                "        .otp-container { background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%); border-radius: 12px; padding: 25px; margin: 25px 0; text-align: center; }\n" +
                "        .otp { font-size: 36px; font-weight: bold; color: #1976D2; letter-spacing: 8px; margin: 0; }\n" +
                "        .otp-label { font-size: 14px; color: #666; margin-bottom: 10px; }\n" +
                "        .info-box { background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0; }\n" +
                "        .info-box p { margin: 0; color: #e65100; font-size: 14px; }\n" +
                "        .footer { background-color: #f5f7fa; padding: 25px; text-align: center; font-size: 13px; color: #666; border-top: 1px solid #e0e0e0; }\n" +
                "        .footer .brand { color: #1976D2; font-weight: bold; font-size: 18px; margin-bottom: 10px; }\n" +
                "        h2 { color: #1976D2; font-size: 20px; margin-top: 0; margin-bottom: 15px; font-weight: 600; }\n" +
                "        p { line-height: 1.6; margin-bottom: 15px; }\n" +
                "        .highlight { color: #1976D2; font-weight: 600; }\n" +
                "        .contact-info { margin-top: 20px; padding-top: 20px; border-top: 1px solid #e0e0e0; }\n" +
                "        .contact-info p { margin: 5px 0; font-size: 14px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <div class=\"logo\">🚚 Truckie</div>\n" +
                "            <h1>Yêu cầu đặt lại mật khẩu</h1>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Xin chào %s,</h2>\n" +
                "            <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã OTP bên dưới để tiếp tục:</p>\n" +
                "            <div class=\"otp-container\">\n" +
                "                <p class=\"otp-label\">Mã xác thực của bạn</p>\n" +
                "                <p class=\"otp\">%s</p>\n" +
                "            </div>\n" +
                "            <div class=\"info-box\">\n" +
                "                <p>⏱️ Mã OTP này có hiệu lực trong <strong>5 phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>\n" +
                "            </div>\n" +
                "            <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này hoặc liên hệ với chúng tôi ngay lập tức.</p>\n" +
                "            <div class=\"contact-info\">\n" +
                "                <p><strong>Cần hỗ trợ?</strong></p>\n" +
                "                <p>📧 Email: support@truckie.vn</p>\n" +
                "                <p>📞 Hotline: 1900 xxxx</p>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <div class=\"brand\">Truckie</div>\n" +
                "            <p>Giải pháp vận tải thông minh</p>\n" +
                "            <p>© 2024 Truckie. Bảo lưu mọi quyền.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}