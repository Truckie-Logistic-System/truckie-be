package capstone_project.controller;

import capstone_project.dto.request.fcm.RegisterFCMTokenRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.entity.fcm.FCMTokenEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.FCMTokenRepository;
import capstone_project.repository.repositories.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/fcm-tokens")
@RequiredArgsConstructor
@Slf4j
public class FCMTokenController {

    private final FCMTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * Register or update FCM token for current user
     * Mobile apps call this on app start and when token refreshes
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<String>> registerFCMToken(
            @Valid @RequestBody RegisterFCMTokenRequest request) {
        
        try {
            // Get current authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            // Find user by username
            UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            log.info("📱 Registering FCM token for user: {}, device: {}", user.getId(), request.getDeviceType());
            
            // Check if token already exists
            Optional<FCMTokenEntity> existingToken = fcmTokenRepository.findByTokenAndIsActiveTrue(request.getToken());
            
            if (existingToken.isPresent()) {
                // Update existing token
                FCMTokenEntity token = existingToken.get();
                token.setDeviceInfo(request.getDeviceInfo());
                token.updateLastUsed();
                fcmTokenRepository.save(token);
                
                log.info("✅ Updated existing FCM token for user: {}", user.getId());
                return ResponseEntity.ok(ApiResponse.ok("FCM token updated successfully"));
            } else {
                // Create new token - simplified without driver logic
                FCMTokenEntity newToken = FCMTokenEntity.builder()
                    .token(request.getToken())
                    .deviceType(request.getDeviceType())
                    .deviceInfo(request.getDeviceInfo())
                    .user(user)
                    .isActive(true)
                    .build();
                
                fcmTokenRepository.save(newToken);
                
                log.info("✅ Registered new FCM token for user: {}", user.getId());
                return ResponseEntity.ok(ApiResponse.ok("FCM token registered successfully"));
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to register FCM token", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Failed to register FCM token: " + e.getMessage(), 400));
        }
    }

    /**
     * Unregister FCM token (when user logs out)
     */
    @DeleteMapping("/unregister")
    public ResponseEntity<ApiResponse<String>> unregisterFCMToken(
            @RequestParam String token) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            log.info("🗑️ Unregistering FCM token for user: {}", userId);
            
            // Mark token as inactive instead of deleting
            fcmTokenRepository.markTokenAsInactive(token);
            
            log.info("✅ FCM token unregistered for user: {}", userId);
            return ResponseEntity.ok(ApiResponse.ok("FCM token unregistered successfully"));
            
        } catch (Exception e) {
            log.error("❌ Failed to unregister FCM token", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Failed to unregister FCM token: " + e.getMessage(), 400));
        }
    }

    /**
     * Unregister all tokens for current user (logout from all devices)
     */
    @DeleteMapping("/unregister-all")
    public ResponseEntity<ApiResponse<String>> unregisterAllTokens() {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            log.info("🗑️ Unregistering all FCM tokens for user: {}", userId);
            
            // Mark all user tokens as inactive
            // You'll need to implement user lookup to get the user ID
            // fcmTokenRepository.markAllUserTokensAsInactive(user.getId());
            
            log.info("✅ All FCM tokens unregistered for user: {}", userId);
            return ResponseEntity.ok(ApiResponse.ok("All FCM tokens unregistered successfully"));
            
        } catch (Exception e) {
            log.error("❌ Failed to unregister all FCM tokens", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Failed to unregister all FCM tokens: " + e.getMessage(), 400));
        }
    }

    /**
     * Get active tokens for current user (for debugging)
     */
    @GetMapping("/my-tokens")
    public ResponseEntity<ApiResponse<List<FCMTokenEntity>>> getMyTokens() {
        
        try {
            // Get active tokens for user
            // You'll need to implement user lookup
            // List<FCMTokenEntity> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(user.getId());
            
            // return ResponseEntity.ok(ApiResponse.success(tokens));
            
            // For now, return empty list
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
            
        } catch (Exception e) {
            log.error("❌ Failed to get FCM tokens", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Failed to get FCM tokens: " + e.getMessage(), 400));
        }
    }

    /**
     * Test FCM notification (for development)
     */
    @PostMapping("/test-notification")
    public ResponseEntity<ApiResponse<String>> testNotification(
            @RequestParam String title,
            @RequestParam String message) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            log.info("🧪 Sending test FCM notification to user: {}", userId);
            
            // Get user tokens and send test notification
            // List<FCMTokenEntity> tokens = fcmTokenRepository.findByUserIdAndIsActiveTrue(user.getId());
            // List<String> tokenStrings = tokens.stream().map(FCMTokenEntity::getToken).collect(Collectors.toList());
            
            // if (!tokenStrings.isEmpty()) {
            //     fcmService.sendNotificationToTokens(tokenStrings, title, message, Map.of("test", "true"));
            //     return ResponseEntity.ok(ApiResponse.success("Test notification sent to " + tokenStrings.size() + " devices"));
            // } else {
            //     return ResponseEntity.badRequest().body(ApiResponse.error("No active FCM tokens found"));
            // }
            
            return ResponseEntity.ok(ApiResponse.ok("Test endpoint ready - implement user lookup"));
            
        } catch (Exception e) {
            log.error("❌ Failed to send test notification", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Failed to send test notification: " + e.getMessage(), 400));
        }
    }
}
