package capstone_project.controller.controllers;


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
    public ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        log.info("Received verify request for email: {}", email);
        boolean isVerified = emailProtocolService.verifyOtp(email, otp);
        if (isVerified) {
            log.info("Finished sending OTP email to {}", email);
            return ResponseEntity.ok("OTP verified successfully");
        } else {
            log.info("Finished sending OTP email to {}", email);
            return ResponseEntity.badRequest().body("Invalid OTP");
        }
    }
}
