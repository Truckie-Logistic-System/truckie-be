package capstone_project.common.template;

public class OtpEmailTemplate {

    public static String getOtpEmailTemplate() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; background-color: #f9f9f9; margin: 0; padding: 20px; }\n" +
                "        .container { max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); overflow: hidden; }\n" +
                "        .header { background-color: #4CAF50; color: white; padding: 25px; text-align: center; border-bottom: 5px solid #FFA500; }\n" +
                "        .header h1 { margin: 0; font-size: 28px; font-weight: bold; }\n" +
                "        .content { padding: 25px; }\n" +
                "        .otp { font-size: 28px; font-weight: bold; color: #006400; text-align: center; margin: 20px 0; padding: 10px; background-color: #b3e6b3; border-radius: 4px; }\n" +
                "        .section-title { font-size: 20px; color: #4CAF50; margin-bottom: 15px; padding-bottom: 10px; font-weight: bold; }\n" +
                "        .footer { background-color: #4CAF50; padding: 20px; text-align: center; font-size: 14px; color: white; border-top: 5px solid #FFD700; }\n" +
                "        h2 { color: #4CAF50; font-size: 22px; margin-top: 25px; margin-bottom: 15px; font-weight: bold; }\n" +
                "        ul { padding-left: 20px; margin-bottom: 20px; }\n" +
                "        ul li { margin-bottom: 10px; color: #555; }\n" +
                "        a { color: #FFA500; text-decoration: none; font-weight: bold; }\n" +
                "        a:hover { text-decoration: underline; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>Cleanee</h1>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Hello!</h2>\n" +
                "            <p>Thank you for choosing <strong>Cleanee</strong> for your home service needs. To complete your registration, please use the OTP below:</p>\n" +
                "            <p class=\"otp\">%s</p>\n" +
                "            <p>This OTP is valid for <strong>5 minutes</strong>. If you did not request this OTP, please ignore this email or contact our support team.</p>\n" +
                "            <h2 class=\"section-title\">Contact Information</h2>\n" +
                "            <p><strong>If you have any questions or need assistance, please don't hesitate to reach out:</strong></p>\n" +
                "            <ul>\n" +
                "                <li><strong>Phone:</strong> 0123 456 789</li>\n" +
                "                <li><strong>Email:</strong> <a href=\"mailto:support@homeservice.com\">support@homeservice.com</a></li>\n" +
                "                <li><strong>Website:</strong> <a href=\"https://www.homeservice.com\">www.homeservice.com</a></li>\n" +
                "            </ul>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>If you have any questions, please contact our support team.</p>\n" +
                "            <p>© 2024 Cleanee. All rights reserved.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}