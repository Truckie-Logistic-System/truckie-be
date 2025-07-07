package capstone_project.template;

public class OtpEmailForgetPasswordTemplate {
    public static String getOtpEmailForgetPasswordTemplate() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; background-color: #f9f9f9; margin: 0; padding: 20px; }\n" +
                "        .container { max-width: 600px; margin: auto; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); overflow: hidden; }\n" +
                "        .header { background-color: #d9534f; color: white; padding: 25px; text-align: center; border-bottom: 5px solid #c9302c; }\n" +
                "        .header h1 { margin: 0; font-size: 28px; font-weight: bold; }\n" +
                "        .content { padding: 25px; }\n" +
                "        .otp { font-size: 28px; font-weight: bold; color: #d9534f; text-align: center; margin: 20px 0; padding: 10px; background-color: #f2dede; border-radius: 4px; }\n" +
                "        .section-title { font-size: 20px; color: #d9534f; margin-bottom: 15px; padding-bottom: 10px; font-weight: bold; }\n" +
                "        .footer { background-color: #d9534f; padding: 20px; text-align: center; font-size: 14px; color: white; border-top: 5px solid #c9302c; }\n" +
                "        h2 { color: #d9534f; font-size: 22px; margin-top: 25px; margin-bottom: 15px; font-weight: bold; }\n" +
                "        ul { padding-left: 20px; margin-bottom: 20px; }\n" +
                "        ul li { margin-bottom: 10px; color: #555; }\n" +
                "        a { color: #c9302c; text-decoration: none; font-weight: bold; }\n" +
                "        a:hover { text-decoration: underline; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>Password Reset Request</h1>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <h2>Hello,</h2>\n" +
                "            <p>We received a request to reset your password for the username <strong>%s</strong>. Use the OTP below to proceed:</p>\n" +
                "            <p class=\"otp\">%s</p>\n" +
                "            <p>This OTP is valid for <strong>5 minutes</strong>. If you did not request this reset, please ignore this email or contact support.</p>\n" +
                "            <h2 class=\"section-title\">Need Help?</h2>\n" +
                "            <p>If you have any issues, reach out to our support team:</p>\n" +
                "            <ul>\n" +
                "                <li><strong>Phone:</strong> 0123 456 789</li>\n" +
                "                <li><strong>Email:</strong> <a href=\"mailto:support@homeservice.com\">support@homeservice.com</a></li>\n" +
                "                <li><strong>Website:</strong> <a href=\"https://www.homeservice.com\">www.homeservice.com</a></li>\n" +
                "            </ul>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>If you need further assistance, contact our support team.</p>\n" +
                "            <p>© 2024 Cleanee. All rights reserved.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}