package capstone_project.controller.controllers;


import capstone_project.controller.dtos.request.OtpVerifyRequest;
import capstone_project.controller.dtos.response.ApiResponse;
import capstone_project.service.services.EmailProtocolService;
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
    public void sendOtpMail(@RequestParam String email, @RequestParam String otp) {
        emailProtocolService.sendOtpEmail(email, otp);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@RequestParam OtpVerifyRequest otpVerifyRequest) {
        log.info("Received verify request for email: {}", otpVerifyRequest.getEmail());
        boolean isVerified = emailProtocolService.verifyOtp(otpVerifyRequest.getEmail(), otpVerifyRequest.getOtp());
        if (isVerified) {
            log.info("Finished sending OTP email to {}", otpVerifyRequest.getEmail());
            return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully"));
        } else {
            log.info("Finished sending OTP email to {}", otpVerifyRequest.getEmail());
            return ResponseEntity.ok(ApiResponse.fail("Invalid OTP", 400));
        }
    }
}
