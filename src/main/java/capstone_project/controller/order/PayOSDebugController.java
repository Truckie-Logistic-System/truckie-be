package capstone_project.controller.order;

import capstone_project.service.services.order.transaction.payOS.impl.DirectPayOSApiTest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Debug controller to test PayOS API directly
 * This bypasses the PayOS SDK to identify if SDK is the issue
 */
@RestController
@RequestMapping("/api/v1/debug/payos")
@RequiredArgsConstructor
@Slf4j
public class PayOSDebugController {

    private final DirectPayOSApiTest directPayOSApiTest;

    @PostMapping("/test-direct-call")
    public ResponseEntity<String> testDirectPayOSCall() {
        
        directPayOSApiTest.testDirectPayOSCall();
        return ResponseEntity.ok("Check server logs for detailed results");
    }
}
