package capstone_project.dtos.response.auth;

import capstone_project.dtos.response.user.CustomerResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRegisterResponse {
    
    private CustomerResponse customer;
    private boolean otpRequired;
    private String email;
    private String otpMessage;
    
    // Static factory method for OTP required response
    public static CustomerRegisterResponse withOtpRequired(CustomerResponse customer, String email) {
        return CustomerRegisterResponse.builder()
                .customer(customer)
                .otpRequired(true)
                .email(email)
                .otpMessage("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra và nhập mã để hoàn tất đăng ký.")
                .build();
    }
    
    // Static factory method for completed registration (no OTP required)
    public static CustomerRegisterResponse completed(CustomerResponse customer) {
        return CustomerRegisterResponse.builder()
                .customer(customer)
                .otpRequired(false)
                .build();
    }
}
