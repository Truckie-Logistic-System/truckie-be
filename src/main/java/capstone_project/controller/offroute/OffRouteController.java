package capstone_project.controller.offroute;

import capstone_project.dtos.request.offroute.ConfirmOffRouteSafeRequest;
import capstone_project.dtos.request.offroute.CreateOffRouteIssueRequest;
import capstone_project.dtos.response.offroute.OffRouteEventDetailResponse;
import capstone_project.entity.offroute.OffRouteEventEntity;
import capstone_project.repository.entityServices.offroute.OffRouteEventEntityService;
import capstone_project.service.services.offroute.OffRouteDetectionService;
import capstone_project.common.utils.UserContextUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/off-route-events")
@RequiredArgsConstructor
@Slf4j
public class OffRouteController {

    private final OffRouteDetectionService offRouteDetectionService;
    private final OffRouteEventEntityService offRouteEventEntityService;
    private final UserContextUtils userContextUtils;

    /**
     * Get full details of an off-route event for staff modal
     */
    @GetMapping("/{eventId}/detail")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<OffRouteEventDetailResponse> getEventDetail(@PathVariable UUID eventId) {
        log.info("[OffRouteController] Getting detail for event: {}", eventId);
        OffRouteEventDetailResponse detail = offRouteDetectionService.getEventDetail(eventId);
        return ResponseEntity.ok(detail);
    }

    /**
     * Staff confirms driver is safe after contact
     */
    @PostMapping("/{eventId}/confirm-safe")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmSafe(
            @PathVariable UUID eventId,
            @Valid @RequestBody ConfirmOffRouteSafeRequest request,
            Authentication authentication) {
        
        log.info("[OffRouteController] Confirming safe for event: {}", eventId);
        
        // Get staff ID from authentication
        UUID staffId = userContextUtils.getCurrentUserId();
        
        OffRouteEventEntity event = offRouteDetectionService.confirmSafe(eventId, request.getNotes(), staffId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã xác nhận tài xế an toàn");
        response.put("eventId", event.getId());
        response.put("status", event.getWarningStatus().name());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Staff marks that driver could not be contacted
     */
    @PostMapping("/{eventId}/mark-no-contact")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> markNoContact(
            @PathVariable UUID eventId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        
        log.info("[OffRouteController] Marking no contact for event: {}", eventId);
        
        UUID staffId = userContextUtils.getCurrentUserId();
        String notes = request != null ? request.get("notes") : null;
        
        OffRouteEventEntity event = offRouteDetectionService.markNoContact(eventId, notes, staffId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã đánh dấu không liên hệ được tài xế");
        response.put("eventId", event.getId());
        response.put("canContactDriver", event.getCanContactDriver());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create an issue from off-route event
     */
    @PostMapping("/create-issue")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createIssue(
            @Valid @RequestBody CreateOffRouteIssueRequest request,
            Authentication authentication) {
        
        log.info("[OffRouteController] Creating issue from event: {}", request.getOffRouteEventId());
        
        UUID staffId = userContextUtils.getCurrentUserId();
        
        UUID issueId = offRouteDetectionService.createIssueFromEvent(
            request.getOffRouteEventId(), 
            request.getDescription(), 
            staffId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã tạo sự cố từ cảnh báo lệch tuyến");
        response.put("issueId", issueId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active off-route events (for staff dashboard)
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<OffRouteEventEntity>> getActiveEvents() {
        log.info("[OffRouteController] Getting all active off-route events");
        List<OffRouteEventEntity> events = offRouteEventEntityService.findAllActiveEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Get off-route events by order ID
     */
    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<List<OffRouteEventEntity>> getByOrderId(@PathVariable UUID orderId) {
        log.info("[OffRouteController] Getting off-route events for order: {}", orderId);
        List<OffRouteEventEntity> events = offRouteEventEntityService.findByOrderId(orderId);
        return ResponseEntity.ok(events);
    }

    /**
     * Staff confirms contact with driver for off-route event
     */
    @PostMapping("/{eventId}/confirm-contact")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmContact(
            @PathVariable UUID eventId,
            Authentication authentication) {
        
        log.info("[OffRouteController] Confirming contact for event: {}", eventId);
        
        UUID staffId = userContextUtils.getCurrentUserId();
        
        OffRouteEventEntity event = offRouteDetectionService.confirmContact(eventId, staffId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã xác nhận liên hệ với tài xế, đang chờ tài xế quay lại tuyến đường");
        response.put("eventId", event.getId());
        response.put("status", event.getWarningStatus().name());
        response.put("gracePeriodExpiresAt", event.getGracePeriodExpiresAt());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Staff extends grace period for driver to return to route
     */
    @PostMapping("/{eventId}/extend-grace-period")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> extendGracePeriod(
            @PathVariable UUID eventId,
            Authentication authentication) {
        
        log.info("[OffRouteController] Extending grace period for event: {}", eventId);
        
        UUID staffId = userContextUtils.getCurrentUserId();
        
        OffRouteEventEntity event = offRouteDetectionService.extendGracePeriod(eventId, staffId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Đã gia hạn thời gian chờ tài xế quay lại tuyến đường");
        response.put("eventId", event.getId());
        response.put("status", event.getWarningStatus().name());
        response.put("gracePeriodExpiresAt", event.getGracePeriodExpiresAt());
        response.put("extensionCount", event.getGracePeriodExtensionCount());
        
        return ResponseEntity.ok(response);
    }
}
