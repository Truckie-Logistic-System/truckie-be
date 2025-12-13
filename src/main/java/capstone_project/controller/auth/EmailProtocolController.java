package capstone_project.controller.auth;

import capstone_project.dtos.request.auth.OtpVerifyRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.services.email.EmailProtocolService;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${email.api.base-path}")
@Slf4j
public class EmailProtocolController {

    private final EmailProtocolService emailProtocolService;

    @GetMapping("/otp/send")
    public void sendOtpMail(@RequestBody OtpVerifyRequest otpVerifyRequest) {
        emailProtocolService.sendOtpEmail(otpVerifyRequest.getEmail(), otpVerifyRequest.getOtp());
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@RequestBody OtpVerifyRequest otpVerifyRequest) {
        
        boolean isVerified = emailProtocolService.verifyOtp(otpVerifyRequest.getEmail(), otpVerifyRequest.getOtp());
        if (isVerified) {
            
            return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully"));
        } else {
            
            return ResponseEntity.ok(ApiResponse.fail("Invalid OTP", 400));
        }
    }
    
    /**
     * Resend OTP to user's email
     * @param otpVerifyRequest Request containing email
     * @return Response with success message
     */
    @PostMapping("/otp/resend")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestBody OtpVerifyRequest otpVerifyRequest) {
        try {
            // Generate new OTP
            String newOtp = String.format("%06d", new Random().nextInt(999999));
            
            // Send new OTP to email
            emailProtocolService.sendOtpEmail(otpVerifyRequest.getEmail(), newOtp);
            
            log.info("[resendOtp] New OTP sent to email: {}", otpVerifyRequest.getEmail());
            return ResponseEntity.ok(ApiResponse.ok("New OTP sent successfully"));
        } catch (Exception e) {
            log.error("[resendOtp] Failed to send new OTP to email: {}", otpVerifyRequest.getEmail(), e);
            return ResponseEntity.ok(ApiResponse.fail("Failed to send new OTP", 500));
        }
    }
}
