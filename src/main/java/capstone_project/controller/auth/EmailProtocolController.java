package capstone_project.controller.auth;

import capstone_project.dtos.request.auth.OtpVerifyRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.services.email.EmailProtocolService;
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
}
