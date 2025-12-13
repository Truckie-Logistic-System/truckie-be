package capstone_project.controller.auth;

import capstone_project.dtos.request.auth.ForgotPasswordRequest;
import capstone_project.dtos.request.auth.ResetPasswordRequest;
import capstone_project.dtos.request.auth.VerifyForgotPasswordOtpRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.services.email.EmailProtocolService;
import capstone_project.service.services.auth.RegisterService;
import capstone_project.dtos.request.auth.ChangePasswordForForgetPassRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("${auth.api.base-path}/forgot-password")
@Slf4j
public class ForgotPasswordController {

    private final EmailProtocolService emailProtocolService;
    private final RegisterService registerService;

    /**
     * Step 1: Send OTP to user's email
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendForgotPasswordOtp(
            @RequestBody @Valid ForgotPasswordRequest request) {
        try {
            emailProtocolService.sendForgotPasswordOtp(request.getEmail());
            return ResponseEntity.ok(ApiResponse.ok("Mã OTP đã được gửi đến email của bạn"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage(), 400));
        } catch (Exception e) {
            log.error("[sendForgotPasswordOtp] Error: ", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Không thể gửi email. Vui lòng thử lại sau.", 500));
        }
    }

    /**
     * Step 2: Verify OTP and get reset token
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyForgotPasswordOtp(
            @RequestBody @Valid VerifyForgotPasswordOtpRequest request) {
        String resetToken = emailProtocolService.verifyForgotPasswordOtp(
                request.getEmail(), 
                request.getOtp()
        );

        if (resetToken == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Mã OTP không hợp lệ hoặc đã hết hạn", 400));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "resetToken", resetToken,
                "message", "Xác thực OTP thành công"
        )));
    }

    /**
     * Step 3: Reset password with reset token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        
        // Validate reset token
        if (!emailProtocolService.validateResetToken(request.getEmail(), request.getResetToken())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Token không hợp lệ hoặc đã hết hạn. Vui lòng thực hiện lại quy trình quên mật khẩu.", 400));
        }

        // Validate password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Mật khẩu xác nhận không khớp", 400));
        }

        try {
            // Get username from email to use existing change password logic
            var changePasswordRequest = new ChangePasswordForForgetPassRequest(
                    null, // username will be looked up by email
                    request.getEmail(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );

            registerService.changePasswordForForgetPassword(changePasswordRequest);

            // Invalidate reset token after successful password change
            emailProtocolService.invalidateResetToken(request.getEmail());

            return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới."));
        } catch (Exception e) {
            log.error("[resetPassword] Error: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage(), 400));
        }
    }
}
