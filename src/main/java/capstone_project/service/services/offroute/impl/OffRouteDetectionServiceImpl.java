package capstone_project.service.services.offroute.impl;

import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.common.enums.IssueEnum;
import capstone_project.common.enums.OffRouteWarningStatus;
import capstone_project.dtos.response.offroute.OffRouteEventDetailResponse;
import capstone_project.dtos.response.offroute.OffRouteEventListResponse;
import capstone_project.dtos.response.offroute.OffRouteWarningPayload;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.offroute.OffRouteEventEntity;
import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.entity.order.order.JourneySegmentEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.issue.IssueTypeEntityService;
import capstone_project.repository.entityServices.offroute.OffRouteEventEntityService;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.repository.repositories.order.contract.ContractRepository;
import capstone_project.repository.repositories.order.order.OrderDetailRepository;
import capstone_project.service.services.offroute.OffRouteDetectionService;
import capstone_project.entity.order.contract.ContractEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import capstone_project.service.services.offroute.OffRouteDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.auth.UserEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class OffRouteDetectionServiceImpl implements OffRouteDetectionService {

    private final OffRouteEventEntityService offRouteEventEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final JourneyHistoryEntityService journeyHistoryEntityService;
    private final OrderDetailRepository orderDetailRepository;
    private final ContractRepository contractRepository;
    private final IssueEntityService issueEntityService;
    private final IssueTypeEntityService issueTypeEntityService;
    private final IssueRepository issueRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Configuration constants
    @Value("${offroute.max-distance-meters:500.0}")
    private double maxDistanceOnRouteMeters;

    // Thời gian cảnh báo lệch tuyến (phút) – cấu hình qua application properties
    @Value("${offroute.warning.yellow-minutes:5}")
    private double yellowWarningMinutes;

    @Value("${offroute.warning.red-minutes:10}")
    private double redWarningMinutes;

    // Contact confirmation flow configuration
    @Value("${offroute.contact.grace-period-minutes:20}")
    private int gracePeriodMinutes;

    @Value("${offroute.contact.extension-minutes:15}")
    private int extensionMinutes;

    @Value("${offroute.return.min-distance-meters:50.0}")
    private double returnMinDistanceMeters;

    private static final String STAFF_OFF_ROUTE_TOPIC = "/topic/staff/off-route-warnings";
    private static final String STAFF_NEW_ISSUES_TOPIC = "/topic/staff/new-issues";

    @Override
    public List<OffRouteEventListResponse> getAllOffRouteEvents() {
        List<OffRouteEventEntity> events = offRouteEventEntityService.findAll();
        
        // Sort by createdAt DESC (newest first)
        events.sort((a, b) -> {
            LocalDateTime aTime = a.getOffRouteStartTime();
            LocalDateTime bTime = b.getOffRouteStartTime();
            if (aTime == null && bTime == null) return 0;
            if (aTime == null) return 1;
            if (bTime == null) return -1;
            return bTime.compareTo(aTime);
        });
        
        return events.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }
    
    private OffRouteEventListResponse mapToListResponse(OffRouteEventEntity event) {
        VehicleAssignmentEntity va = event.getVehicleAssignment();
        OrderEntity order = event.getOrder();
        IssueEntity issue = event.getIssue();
        
        // Get driver info safely
        String driverName = null;
        String driverPhone = null;
        if (va != null && va.getDriver1() != null) {
            DriverEntity driver = va.getDriver1();
            UserEntity driverUser = driver.getUser();
            if (driverUser != null) {
                driverName = driverUser.getFullName();
                driverPhone = driverUser.getPhoneNumber();
            }
        }
        
        // Get vehicle plate number safely
        String vehiclePlateNumber = null;
        if (va != null && va.getVehicleEntity() != null) {
            vehiclePlateNumber = va.getVehicleEntity().getLicensePlateNumber();
        }
        
        // Get sender name safely
        String senderName = null;
        if (order != null && order.getSender() != null && order.getSender().getUser() != null) {
            senderName = order.getSender().getUser().getFullName();
        }
        
        return OffRouteEventListResponse.builder()
                .id(event.getId())
                .offRouteStartTime(event.getOffRouteStartTime())
                .lastKnownLat(event.getLastKnownLat())
                .lastKnownLng(event.getLastKnownLng())
                .distanceFromRouteMeters(event.getDistanceFromRouteMeters())
                .warningStatus(event.getWarningStatus())
                .yellowWarningSentAt(event.getYellowWarningSentAt())
                .redWarningSentAt(event.getRedWarningSentAt())
                .canContactDriver(event.getCanContactDriver())
                .contactedAt(event.getContactedAt())
                .resolvedAt(event.getResolvedAt())
                .resolvedReason(event.getResolvedReason())
                .gracePeriodExpiresAt(event.getGracePeriodExpiresAt())
                .gracePeriodExtensionCount(event.getGracePeriodExtensionCount())
                .createdAt(event.getOffRouteStartTime())
                .vehicleAssignment(va != null ? OffRouteEventListResponse.VehicleAssignmentInfo.builder()
                        .id(va.getId())
                        .trackingCode(va.getTrackingCode())
                        .status(va.getStatus())
                        .vehiclePlateNumber(vehiclePlateNumber)
                        .driverName(driverName)
                        .driverPhone(driverPhone)
                        .build() : null)
                .order(order != null ? OffRouteEventListResponse.OrderInfo.builder()
                        .id(order.getId())
                        .orderCode(order.getOrderCode())
                        .status(order.getStatus())
                        .senderName(senderName)
                        .receiverName(order.getReceiverName())
                        .build() : null)
                .issue(issue != null ? OffRouteEventListResponse.IssueInfo.builder()
                        .id(issue.getId())
                        .issueTypeName(issue.getIssueTypeEntity() != null ? 
                                issue.getIssueTypeEntity().getIssueTypeName() : null)
                        .status(issue.getStatus())
                        .build() : null)
                .build();
    }

    @Override
    @Transactional
    public Double processLocationUpdate(UUID vehicleAssignmentId, BigDecimal lat, BigDecimal lng,
                                       Double speed, Double bearing) {
        try {
            // Get vehicle assignment with journey
            Optional<VehicleAssignmentEntity> assignmentOpt = 
                vehicleAssignmentEntityService.findById(vehicleAssignmentId);
            
            if (assignmentOpt.isEmpty()) {
                log.warn("[OffRoute] Vehicle assignment not found: {}", vehicleAssignmentId);
                return null;
            }
            
            VehicleAssignmentEntity assignment = assignmentOpt.get();
            
            // Get active journey for this assignment
            Optional<JourneyHistoryEntity> journeyOpt = 
                journeyHistoryEntityService.findLatestActiveJourney(vehicleAssignmentId);
            
            if (journeyOpt.isEmpty() || journeyOpt.get().getJourneySegments() == null) {
                log.debug("[OffRoute] No active journey found for assignment: {}", vehicleAssignmentId);
                return null;
            }
            
            JourneyHistoryEntity journey = journeyOpt.get();
            List<JourneySegmentEntity> segments = journey.getJourneySegments();
            
            // Calculate distance to route
            double distanceToRoute = calculateDistanceToRoute(
                lat.doubleValue(), lng.doubleValue(), segments);
            
            log.debug("[OffRoute] Distance to route for {}: {} meters", vehicleAssignmentId, distanceToRoute);
            
            // Check if off-route
            if (distanceToRoute > maxDistanceOnRouteMeters) {
                handleOffRoute(assignment, lat, lng, distanceToRoute);
            } else {
                // Back on route - reset if there was an active event
                handleBackOnRoute(vehicleAssignmentId);
            }
            
            // Return calculated distance for real-time WebSocket updates
            return distanceToRoute;
            
        } catch (Exception e) {
            log.error("[OffRoute] Error processing location update for {}: {}", 
                vehicleAssignmentId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Handle off-route condition
     */
    private void handleOffRoute(VehicleAssignmentEntity assignment, 
                                 BigDecimal lat, BigDecimal lng, double distanceToRoute) {
        UUID assignmentId = assignment.getId();
        
        // Check for existing active event
        Optional<OffRouteEventEntity> existingEventOpt = 
            offRouteEventEntityService.findActiveByVehicleAssignmentId(assignmentId);
        
        if (existingEventOpt.isPresent()) {
            // Update existing event only if still active
            OffRouteEventEntity event = existingEventOpt.get();
            
            // CRITICAL FIX: Check if event is still active before updating
            // This prevents tracking after ISSUE_CREATED status
            if (!event.isActive()) {
                log.debug("[OffRoute] Skipping update for inactive event {} with status {}", 
                    event.getId(), event.getWarningStatus());
                return;
            }
            
            // Store previous distance before updating
            event.setPreviousDistanceFromRouteMeters(event.getDistanceFromRouteMeters());
            event.setLastLocationUpdateAt(LocalDateTime.now());
            
            event.setLastKnownLat(lat);
            event.setLastKnownLng(lng);
            event.setDistanceFromRouteMeters(distanceToRoute);
            offRouteEventEntityService.save(event);
            
            // Check warning thresholds
            checkWarningThresholds(event);
        } else {
            // Create new off-route event
            OrderEntity order = findOrderForAssignment(assignment);
            
            OffRouteEventEntity newEvent = OffRouteEventEntity.builder()
                .vehicleAssignment(assignment)
                .order(order)
                .offRouteStartTime(LocalDateTime.now())
                .lastKnownLat(lat)
                .lastKnownLng(lng)
                .distanceFromRouteMeters(distanceToRoute)
                .previousDistanceFromRouteMeters(null) // No previous distance for new event
                .lastLocationUpdateAt(LocalDateTime.now())
                .warningStatus(OffRouteWarningStatus.NONE)
                .canContactDriver(null)
                .gracePeriodExtended(false)
                .gracePeriodExtensionCount(0)
                .build();
            
            offRouteEventEntityService.save(newEvent);
            log.info("[OffRoute] New off-route event created for assignment: {}", assignmentId);
        }
    }

    /**
     * Handle returning to route
     */
    private void handleBackOnRoute(UUID vehicleAssignmentId) {
        Optional<OffRouteEventEntity> eventOpt = 
            offRouteEventEntityService.findActiveByVehicleAssignmentId(vehicleAssignmentId);
        
        if (eventOpt.isPresent()) {
            OffRouteEventEntity event = eventOpt.get();
            event.setWarningStatus(OffRouteWarningStatus.BACK_ON_ROUTE);
            event.setResolvedAt(LocalDateTime.now());
            event.setResolvedReason("Driver returned to planned route automatically");
            offRouteEventEntityService.save(event);
            
            log.info("[OffRoute] Driver returned to route for assignment: {}", vehicleAssignmentId);
        }
    }

    /**
     * Check warning thresholds and send warnings if needed
     * Simplified flow:
     * - NONE -> YELLOW after yellowWarningMinutes
     * - YELLOW -> RED after redWarningMinutes (from start)
     * - After confirm contact on RED: reset to YELLOW, then RED again after redWarningMinutes from last contact
     */
    private void checkWarningThresholds(OffRouteEventEntity event) {
        // CRITICAL FIX: Check if event is still active before processing warnings
        // This prevents sending warnings for ISSUE_CREATED or other inactive events
        if (!event.isActive()) {
            log.debug("[OffRoute] Skipping warning check for inactive event {} with status {}", 
                event.getId(), event.getWarningStatus());
            return;
        }
        
        long durationMinutes = event.getOffRouteDurationMinutes();
        OffRouteWarningStatus currentStatus = event.getWarningStatus();

        // Calculate time since last contact (if any) for re-sending RED
        long minutesSinceLastContact = 0;
        if (event.getContactedAt() != null) {
            minutesSinceLastContact = Duration.between(event.getContactedAt(), LocalDateTime.now()).toMinutes();
        }

        // Check for RED warning
        if (currentStatus == OffRouteWarningStatus.YELLOW_SENT) {
            // If never contacted: use total duration
            // If contacted before: use time since last contact
            boolean shouldSendRed = false;
            
            if (event.getContactedAt() == null) {
                // Never contacted - use total off-route duration
                shouldSendRed = durationMinutes >= redWarningMinutes;
            } else {
                // Was contacted before - use time since last contact
                shouldSendRed = minutesSinceLastContact >= redWarningMinutes;
            }
            
            if (shouldSendRed) {
                event.setWarningStatus(OffRouteWarningStatus.RED_SENT);
                event.setRedWarningSentAt(LocalDateTime.now());
                offRouteEventEntityService.save(event);
                
                sendWarningToStaff(event, "RED");
                log.warn("[OffRoute] RED warning sent for assignment: {}", event.getVehicleAssignment().getId());
            }
        }
        // Check for YELLOW warning (only from NONE status)
        else if (durationMinutes >= yellowWarningMinutes && currentStatus == OffRouteWarningStatus.NONE) {
            event.setWarningStatus(OffRouteWarningStatus.YELLOW_SENT);
            event.setYellowWarningSentAt(LocalDateTime.now());
            offRouteEventEntityService.save(event);
            
            sendWarningToStaff(event, "YELLOW");
            log.warn("[OffRoute] YELLOW warning sent for assignment: {}", event.getVehicleAssignment().getId());
        }
    }

    /**
     * Send warning to staff via WebSocket
     */
    private void sendWarningToStaff(OffRouteEventEntity event, String severity) {
        try {
            VehicleAssignmentEntity assignment = event.getVehicleAssignment();
            OrderEntity order = event.getOrder();
            VehicleEntity vehicle = assignment.getVehicleEntity();
            
            // Get order details for this assignment
            List<OrderDetailEntity> orderDetails = orderDetailRepository
                .findByVehicleAssignmentEntityId(assignment.getId());
            
            // Calculate totals
            BigDecimal totalDeclaredValue = orderDetails.stream()
                .map(od -> od.getDeclaredValue() != null ? od.getDeclaredValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Build payload
            OffRouteWarningPayload payload = OffRouteWarningPayload.builder()
                .type("OFF_ROUTE_WARNING")
                .severity(severity)
                .offRouteEventId(event.getId())
                .vehicleAssignmentId(assignment.getId())
                .orderId(order.getId())
                .offRouteDurationMinutes(event.getOffRouteDurationMinutes())
                .lastKnownLocation(OffRouteWarningPayload.LocationInfo.builder()
                    .lat(event.getLastKnownLat())
                    .lng(event.getLastKnownLng())
                    .distanceFromRouteMeters(event.getDistanceFromRouteMeters())
                    .build())
                .driverName(assignment.getDriver1() != null ? 
                    assignment.getDriver1().getUser().getFullName() : "N/A")
                .driverPhone(assignment.getDriver1() != null ? 
                    assignment.getDriver1().getUser().getPhoneNumber() : "N/A")
                .vehiclePlate(vehicle != null ? vehicle.getLicensePlateNumber() : "N/A")
                .vehicleType(vehicle != null && vehicle.getVehicleTypeEntity() != null ? 
                    vehicle.getVehicleTypeEntity().getVehicleTypeName() : "N/A")
                .orderCode(order.getOrderCode())
                .packageCount(orderDetails.size())
                .totalContractAmount(BigDecimal.ZERO) // Contract value calculated separately
                .totalDeclaredValue(totalDeclaredValue)
                .senderName(order.getSender() != null ? 
                    order.getSender().getRepresentativeName() : "N/A")
                .receiverName(order.getReceiverName() != null ? 
                    order.getReceiverName() : "N/A")
                .warningTime(LocalDateTime.now())
                .build();
            
            // Send to staff WebSocket topic
            messagingTemplate.convertAndSend(STAFF_OFF_ROUTE_TOPIC, payload);
            log.info("[OffRoute] {} warning sent to staff for order: {}", severity, order.getOrderCode());
            
        } catch (Exception e) {
            log.error("[OffRoute] Error sending warning to staff: {}", e.getMessage(), e);
        }
    }

    @Override
    public OffRouteEventDetailResponse getEventDetail(UUID eventId) {
        OffRouteEventEntity event = offRouteEventEntityService.findByIdWithFullDetails(eventId)
            .orElseThrow(() -> new RuntimeException("Off-route event not found: " + eventId));
        
        return buildEventDetailResponse(event);
    }

    @Override
    @Transactional
    public OffRouteEventEntity confirmSafe(UUID eventId, String notes, UUID staffId) {
        OffRouteEventEntity event = offRouteEventEntityService.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Off-route event not found: " + eventId));
        
        event.setWarningStatus(OffRouteWarningStatus.RESOLVED_SAFE);
        event.setResolvedAt(LocalDateTime.now());
        event.setResolvedReason(notes != null ? notes : "Staff confirmed driver is safe");
        event.setCanContactDriver(true);
        event.setContactNotes(notes);
        
        return offRouteEventEntityService.save(event);
    }

    @Override
    @Transactional
    public OffRouteEventEntity confirmContact(UUID eventId, UUID staffId) {
        OffRouteEventEntity event = offRouteEventEntityService.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Off-route event not found: " + eventId));
        
        if (event.getWarningStatus() != OffRouteWarningStatus.YELLOW_SENT && 
            event.getWarningStatus() != OffRouteWarningStatus.RED_SENT) {
            throw new RuntimeException("Can only confirm contact for events with warning status");
        }
        
        // Simplified flow: Just update contact timestamp, keep tracking
        // Don't change status - allow system to continue monitoring and send RED again if still off-route
        event.setContactedAt(LocalDateTime.now());
        event.setContactedBy(staffId);
        event.setCanContactDriver(true);
        
        // Reset warning sent times to allow re-sending after contact
        // This enables the "loop" behavior: confirm contact -> still off-route -> RED again
        if (event.getWarningStatus() == OffRouteWarningStatus.RED_SENT) {
            // After confirming contact on RED, reset to YELLOW_SENT so RED can be sent again
            event.setWarningStatus(OffRouteWarningStatus.YELLOW_SENT);
            event.setRedWarningSentAt(null);
        }
        
        log.info("[OffRoute] Contact confirmed for event {} by staff {}, continuing to track", 
            eventId, staffId);
        
        return offRouteEventEntityService.save(event);
    }

    @Override
    @Transactional
    public OffRouteEventEntity extendGracePeriod(UUID eventId, UUID staffId) {
        OffRouteEventEntity event = offRouteEventEntityService.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Off-route event not found: " + eventId));
        
        if (event.getWarningStatus() != OffRouteWarningStatus.CONTACTED_WAITING_RETURN) {
            throw new RuntimeException("Can only extend grace period for events waiting for return");
        }
        
        if (event.getGracePeriodExtensionCount() >= 3) {
            throw new RuntimeException("Maximum grace period extensions reached (3)");
        }
        
        event.setGracePeriodExtended(true);
        event.setGracePeriodExtensionCount(event.getGracePeriodExtensionCount() + 1);
        event.setGracePeriodExtendedAt(LocalDateTime.now());
        event.setGracePeriodExpiresAt(event.getGracePeriodExpiresAt().plusMinutes(extensionMinutes));
        
        log.info("[OffRoute] Grace period extended for event {} by staff {}, new expiry at {}, extension count: {}", 
            eventId, staffId, event.getGracePeriodExpiresAt(), event.getGracePeriodExtensionCount());
        
        return offRouteEventEntityService.save(event);
    }

    private boolean isDriverReturningToRoute(OffRouteEventEntity event) {
        if (event.getDistanceFromRouteMeters() == null || event.getPreviousDistanceFromRouteMeters() == null) {
            return false;
        }
        
        // Check if distance is decreasing (driver heading back to route)
        boolean isDecreasing = event.getDistanceFromRouteMeters() < event.getPreviousDistanceFromRouteMeters();
        
        // Check if current distance is within acceptable return threshold
        boolean withinThreshold = event.getDistanceFromRouteMeters() <= returnMinDistanceMeters;
        
        return isDecreasing && withinThreshold;
    }

    @Override
    @Transactional
    // Removed @Scheduled - no auto-create issue, staff must manually create
    public void checkContactedWaitingReturnEvents() {
        // This method is now disabled - no automatic issue creation
        // Staff must manually click "Tạo sự cố" to create issues
        log.debug("[OffRoute] checkContactedWaitingReturnEvents is disabled - manual issue creation only");
    }

    private void createOffRouteRunawayIssue(OffRouteEventEntity event) {
        try {
            // Find issue type by category OFF_ROUTE_RUNAWAY
            IssueTypeEntity issueType = issueTypeEntityService.findByIssueCategory("OFF_ROUTE_RUNAWAY");
            if (issueType == null) {
                throw new RuntimeException("Issue type with category OFF_ROUTE_RUNAWAY not found");
            }
            
            // Create issue for off-route runaway scenario
            IssueEntity issue = new IssueEntity();
            issue.setDescription(String.format(
                "Driver failed to return to route within grace period after staff contact. " +
                "Order: %s, Vehicle: %s, Distance from route: %.2f meters",
                event.getOrder().getOrderCode(),
                event.getVehicleAssignment().getVehicleEntity().getLicensePlateNumber(),
                event.getDistanceFromRouteMeters()
            ));
            issue.setStatus("OPEN");
            issue.setIssueTypeEntity(issueType);
            issue.setVehicleAssignmentEntity(event.getVehicleAssignment());
            issue.setReportedAt(LocalDateTime.now());
            
            issue = issueRepository.save(issue);
            event.setIssue(issue);
            
            // Send WebSocket notification for escalation
            sendEscalationNotification(event);
            
            // Send notification for new issue created
            sendNewIssueNotification(issue, event);
            
        } catch (Exception e) {
            log.error("[OffRoute] Error creating runaway issue for event {}: {}", 
                event.getId(), e.getMessage(), e);
        }
    }

    private void sendEscalationNotification(OffRouteEventEntity event) {
        try {
            OffRouteWarningPayload payload = OffRouteWarningPayload.builder()
                .type("ESCALATION")
                .severity("ESCALATION")
                .offRouteEventId(event.getId())
                .orderId(event.getOrder().getId())
                .orderCode(event.getOrder().getOrderCode())
                .vehicleAssignmentId(event.getVehicleAssignment().getId())
                .driverName(event.getVehicleAssignment().getDriver1() != null ? 
                    event.getVehicleAssignment().getDriver1().getUser().getFullName() : "N/A")
                .driverPhone(event.getVehicleAssignment().getDriver1() != null ? 
                    event.getVehicleAssignment().getDriver1().getUser().getPhoneNumber() : "N/A")
                .vehiclePlate(event.getVehicleAssignment().getVehicleEntity().getLicensePlateNumber())
                .warningTime(LocalDateTime.now())
                .build();
            
            messagingTemplate.convertAndSend(STAFF_OFF_ROUTE_TOPIC, payload);
            log.info("[OffRoute] Escalation notification sent for event {}", event.getId());
            
        } catch (Exception e) {
            log.error("[OffRoute] Error sending escalation notification: {}", e.getMessage(), e);
        }
    }

    private void sendNewIssueNotification(IssueEntity issue, OffRouteEventEntity event) {
        try {
            // Create payload for new issue notification
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "NEW_ISSUE");
            payload.put("issueId", issue.getId());
            payload.put("issueType", issue.getIssueTypeEntity().getIssueTypeName());
            payload.put("description", issue.getDescription());
            payload.put("status", issue.getStatus());
            payload.put("reportedAt", issue.getReportedAt().toString());
            payload.put("orderId", event.getOrder().getId());
            payload.put("orderCode", event.getOrder().getOrderCode());
            payload.put("vehicleAssignmentId", event.getVehicleAssignment().getId());
            payload.put("vehiclePlate", event.getVehicleAssignment().getVehicleEntity().getLicensePlateNumber());
            payload.put("driverName", event.getVehicleAssignment().getDriver1() != null ? 
                event.getVehicleAssignment().getDriver1().getUser().getFullName() : "N/A");
            payload.put("offRouteEventId", event.getId());
            payload.put("autoCreated", true);
            payload.put("locationLatitude", event.getLastKnownLat());
            payload.put("locationLongitude", event.getLastKnownLng());
            
            messagingTemplate.convertAndSend(STAFF_NEW_ISSUES_TOPIC, payload);
            log.info("[OffRoute] New issue notification sent for issue {} from event {}", issue.getId(), event.getId());
            
        } catch (Exception e) {
            log.error("[OffRoute] Error sending new issue notification: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public OffRouteEventEntity markNoContact(UUID eventId, String notes, UUID staffId) {
        OffRouteEventEntity event = offRouteEventEntityService.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Off-route event not found: " + eventId));
        
        event.setCanContactDriver(false);
        event.setLastContactAttemptAt(LocalDateTime.now());
        event.setContactNotes(notes);
        
        return offRouteEventEntityService.save(event);
    }

    @Override
    @Transactional
    public UUID createIssueFromEvent(UUID eventId, String description, UUID staffId) {
        OffRouteEventEntity event = offRouteEventEntityService.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Off-route event not found: " + eventId));
        
        VehicleAssignmentEntity assignment = event.getVehicleAssignment();
        
        // Find issue type by category OFF_ROUTE_RUNAWAY
        IssueTypeEntity issueType = issueTypeEntityService.findByIssueCategory("OFF_ROUTE_RUNAWAY");
        if (issueType == null) {
            throw new RuntimeException("Issue type with category OFF_ROUTE_RUNAWAY not found");
        }
        
        // Build detailed Vietnamese description
        String driverName = assignment.getDriver1() != null ? 
            assignment.getDriver1().getUser().getFullName() : "Không xác định";
        String vehiclePlate = assignment.getVehicleEntity() != null ? 
            assignment.getVehicleEntity().getLicensePlateNumber() : "Không xác định";
        String orderCode = event.getOrder() != null ? 
            event.getOrder().getOrderCode() : "Không xác định";
        
        String detailedDescription = description != null ? description : String.format(
            "SỰ CỐ LỆCH TUYẾN NGHIÊM TRỌNG\n\n" +
            "📍 Thông tin sự kiện:\n" +
            "• Tài xế: %s\n" +
            "• Biển số xe: %s\n" +
            "• Mã đơn hàng: %s\n" +
            "• Thời gian lệch tuyến: %d phút\n" +
            "• Khoảng cách lệch khỏi tuyến: %.0f mét\n" +
            "• Số lần đã liên hệ: %d lần\n\n" +
            "⚠️ Nhân viên đã xác nhận liên hệ nhưng tài xế vẫn tiếp tục lệch tuyến.\n" +
            "Cần xử lý khẩn cấp.",
            driverName,
            vehiclePlate,
            orderCode,
            event.getOffRouteDurationMinutes(),
            event.getDistanceFromRouteMeters() != null ? event.getDistanceFromRouteMeters() : 0.0,
            event.getContactedAt() != null ? 1 : 0
        );
        
        // Create issue
        IssueEntity issue = IssueEntity.builder()
            .description(detailedDescription)
            .locationLatitude(event.getLastKnownLat())
            .locationLongitude(event.getLastKnownLng())
            .status(IssueEnum.OPEN.name())
            .reportedAt(LocalDateTime.now())
            .vehicleAssignmentEntity(assignment)
            .issueTypeEntity(issueType)
            .build();
        
        IssueEntity savedIssue = issueEntityService.save(issue);
        
        // Update off-route event
        event.setWarningStatus(OffRouteWarningStatus.ISSUE_CREATED);
        event.setIssue(savedIssue);
        offRouteEventEntityService.save(event);
        
        log.info("[OffRoute] Issue created from off-route event: {}", savedIssue.getId());
        
        return savedIssue.getId();
    }

    @Override
    public void checkAndSendWarnings() {
        List<OffRouteEventEntity> activeEvents = offRouteEventEntityService.findAllActiveEvents();
        
        for (OffRouteEventEntity event : activeEvents) {
            try {
                checkWarningThresholds(event);
            } catch (Exception e) {
                log.error("[OffRoute] Error checking warning for event {}: {}", 
                    event.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void resetOffRouteEvent(UUID vehicleAssignmentId) {
        handleBackOnRoute(vehicleAssignmentId);
    }

    /**
     * Calculate minimum distance from a point to a route (polyline)
     */
    private double calculateDistanceToRoute(double lat, double lng, List<JourneySegmentEntity> segments) {
        double minDistance = Double.MAX_VALUE;
        
        for (JourneySegmentEntity segment : segments) {
            // Check distance to segment endpoints
            if (segment.getStartLatitude() != null && segment.getStartLongitude() != null) {
                double distToStart = haversineDistance(
                    lat, lng, 
                    segment.getStartLatitude().doubleValue(), 
                    segment.getStartLongitude().doubleValue());
                minDistance = Math.min(minDistance, distToStart);
            }
            
            if (segment.getEndLatitude() != null && segment.getEndLongitude() != null) {
                double distToEnd = haversineDistance(
                    lat, lng, 
                    segment.getEndLatitude().doubleValue(), 
                    segment.getEndLongitude().doubleValue());
                minDistance = Math.min(minDistance, distToEnd);
            }
            
            // Check distance to path coordinates if available
            if (segment.getPathCoordinatesJson() != null) {
                try {
                    List<List<Double>> coords = objectMapper.readValue(
                        segment.getPathCoordinatesJson(), 
                        new TypeReference<List<List<Double>>>() {});
                    
                    for (int i = 0; i < coords.size() - 1; i++) {
                        List<Double> point1 = coords.get(i);
                        List<Double> point2 = coords.get(i + 1);
                        
                        if (point1.size() >= 2 && point2.size() >= 2) {
                            double distToSegment = pointToSegmentDistance(
                                lat, lng,
                                point1.get(1), point1.get(0), // lat, lng
                                point2.get(1), point2.get(0)  // lat, lng
                            );
                            minDistance = Math.min(minDistance, distToSegment);
                        }
                    }
                } catch (Exception e) {
                    log.debug("[OffRoute] Could not parse path coordinates: {}", e.getMessage());
                }
            }
        }
        
        return minDistance == Double.MAX_VALUE ? 0 : minDistance;
    }

    /**
     * Calculate distance from a point to a line segment using Haversine formula
     */
    private double pointToSegmentDistance(double lat, double lng, 
                                           double lat1, double lng1, 
                                           double lat2, double lng2) {
        // Project point onto line segment and calculate distance
        double dx = lat2 - lat1;
        double dy = lng2 - lng1;
        
        if (dx == 0 && dy == 0) {
            // Segment is a point
            return haversineDistance(lat, lng, lat1, lng1);
        }
        
        double t = Math.max(0, Math.min(1, ((lat - lat1) * dx + (lng - lng1) * dy) / (dx * dx + dy * dy)));
        
        double projLat = lat1 + t * dx;
        double projLng = lng1 + t * dy;
        
        return haversineDistance(lat, lng, projLat, projLng);
    }

    /**
     * Calculate Haversine distance between two points in meters
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // Earth's radius in meters
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * Find order for a vehicle assignment
     */
    private OrderEntity findOrderForAssignment(VehicleAssignmentEntity assignment) {
        List<OrderDetailEntity> orderDetails = orderDetailRepository
            .findByVehicleAssignmentEntityId(assignment.getId());
        
        if (!orderDetails.isEmpty()) {
            return orderDetails.get(0).getOrderEntity();
        }
        
        throw new RuntimeException("No order found for assignment: " + assignment.getId());
    }

    /**
     * Build full detail response for off-route event
     */
    private OffRouteEventDetailResponse buildEventDetailResponse(OffRouteEventEntity event) {
        VehicleAssignmentEntity assignment = event.getVehicleAssignment();
        OrderEntity order = event.getOrder();
        VehicleEntity vehicle = assignment.getVehicleEntity();
        
        // Get order details for this assignment
        List<OrderDetailEntity> orderDetails = orderDetailRepository
            .findByVehicleAssignmentEntityId(assignment.getId());
        
        // Get contract for the order
        Optional<ContractEntity> contractOpt = contractRepository.findContractEntityByOrderEntity_Id(order.getId());
        BigDecimal contractAmount = BigDecimal.ZERO;
        if (contractOpt.isPresent()) {
            ContractEntity contract = contractOpt.get();
            // Use adjustedValue if > 0, otherwise use totalValue
            if (contract.getAdjustedValue() != null && contract.getAdjustedValue().compareTo(BigDecimal.ZERO) > 0) {
                contractAmount = contract.getAdjustedValue();
            } else if (contract.getTotalValue() != null) {
                contractAmount = contract.getTotalValue();
            }
        }
        
        // Get journey segments
        List<OffRouteEventDetailResponse.RouteSegmentInfo> routeSegments = new ArrayList<>();
        Optional<JourneyHistoryEntity> journeyOpt = 
            journeyHistoryEntityService.findLatestActiveJourney(assignment.getId());
        
        if (journeyOpt.isPresent()) {
            for (JourneySegmentEntity seg : journeyOpt.get().getJourneySegments()) {
                routeSegments.add(OffRouteEventDetailResponse.RouteSegmentInfo.builder()
                    .segmentOrder(seg.getSegmentOrder())
                    .startPointName(seg.getStartPointName())
                    .endPointName(seg.getEndPointName())
                    .startLat(seg.getStartLatitude())
                    .startLng(seg.getStartLongitude())
                    .endLat(seg.getEndLatitude())
                    .endLng(seg.getEndLongitude())
                    .pathCoordinatesJson(seg.getPathCoordinatesJson())
                    .build());
            }
        }
        
        // Build package info list
        List<OffRouteEventDetailResponse.PackageInfo> packages = orderDetails.stream()
            .map(od -> OffRouteEventDetailResponse.PackageInfo.builder()
                .orderDetailId(od.getId())
                .trackingCode(od.getTrackingCode())
                .description(od.getDescription())
                .weight(od.getWeightBaseUnit())
                .weightUnit(od.getUnit())
                .status(od.getStatus())
                .declaredValue(od.getDeclaredValue())
                .build())
            .toList();
        
        // Calculate total declared value
        BigDecimal totalDeclaredValue = orderDetails.stream()
            .map(od -> od.getDeclaredValue() != null ? od.getDeclaredValue() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Build driver1 info
        OffRouteEventDetailResponse.DriverInfo driver1Info = null;
        if (assignment.getDriver1() != null) {
            driver1Info = OffRouteEventDetailResponse.DriverInfo.builder()
                .driverId(assignment.getDriver1().getId())
                .fullName(assignment.getDriver1().getUser() != null ? 
                    assignment.getDriver1().getUser().getFullName() : null)
                .phoneNumber(assignment.getDriver1().getUser() != null ? 
                    assignment.getDriver1().getUser().getPhoneNumber() : null)
                .licenseNumber(assignment.getDriver1().getDriverLicenseNumber())
                .build();
        }
        
        // Build driver2 info
        OffRouteEventDetailResponse.DriverInfo driver2Info = null;
        if (assignment.getDriver2() != null) {
            driver2Info = OffRouteEventDetailResponse.DriverInfo.builder()
                .driverId(assignment.getDriver2().getId())
                .fullName(assignment.getDriver2().getUser() != null ? 
                    assignment.getDriver2().getUser().getFullName() : null)
                .phoneNumber(assignment.getDriver2().getUser() != null ? 
                    assignment.getDriver2().getUser().getPhoneNumber() : null)
                .licenseNumber(assignment.getDriver2().getDriverLicenseNumber())
                .build();
        }
        
        // Build address strings
        String pickupAddress = order.getPickupAddress() != null ? 
            String.format("%s, %s, %s", 
                order.getPickupAddress().getStreet() != null ? order.getPickupAddress().getStreet() : "",
                order.getPickupAddress().getWard() != null ? order.getPickupAddress().getWard() : "",
                order.getPickupAddress().getProvince() != null ? order.getPickupAddress().getProvince() : ""
            ).replaceAll("^, |, $", "").replaceAll(", ,", ",") : null;
            
        String deliveryAddress = order.getDeliveryAddress() != null ? 
            String.format("%s, %s, %s", 
                order.getDeliveryAddress().getStreet() != null ? order.getDeliveryAddress().getStreet() : "",
                order.getDeliveryAddress().getWard() != null ? order.getDeliveryAddress().getWard() : "",
                order.getDeliveryAddress().getProvince() != null ? order.getDeliveryAddress().getProvince() : ""
            ).replaceAll("^, |, $", "").replaceAll(", ,", ",") : null;
        
        return OffRouteEventDetailResponse.builder()
            .id(event.getId())
            .warningStatus(event.getWarningStatus().name())
            .offRouteDurationMinutes(event.getOffRouteDurationMinutes())
            .offRouteStartTime(event.getOffRouteStartTime())
            .canContactDriver(event.getCanContactDriver())
            .contactNotes(event.getContactNotes())
            .currentLocation(OffRouteEventDetailResponse.LocationInfo.builder()
                .lat(event.getLastKnownLat())
                .lng(event.getLastKnownLng())
                .distanceFromRouteMeters(event.getDistanceFromRouteMeters())
                .build())
            .plannedRouteSegments(routeSegments)
            .tripInfo(OffRouteEventDetailResponse.TripInfo.builder()
                .vehicleAssignmentId(assignment.getId())
                .trackingCode(assignment.getTrackingCode())
                .status(assignment.getStatus())
                .build())
            // Primary driver info (for backward compatibility)
            .driverInfo(driver1Info)
            // Both drivers
            .driver1Info(driver1Info)
            .driver2Info(driver2Info)
            .vehicleInfo(OffRouteEventDetailResponse.VehicleInfo.builder()
                .vehicleId(vehicle != null ? vehicle.getId() : null)
                .licensePlate(vehicle != null ? vehicle.getLicensePlateNumber() : null)
                .vehicleType(vehicle != null && vehicle.getVehicleTypeEntity() != null ? 
                    vehicle.getVehicleTypeEntity().getVehicleTypeName() : null)
                .vehicleTypeDescription(vehicle != null && vehicle.getVehicleTypeEntity() != null ? 
                    vehicle.getVehicleTypeEntity().getDescription() : null)
                .manufacturer(vehicle != null ? vehicle.getManufacturer() : null)
                .model(vehicle != null ? vehicle.getModel() : null)
                .yearOfManufacture(vehicle != null ? vehicle.getYear() : null)
                .loadCapacityKg(vehicle != null && vehicle.getVehicleTypeEntity() != null ? 
                    vehicle.getVehicleTypeEntity().getWeightLimitTon() != null ?
                        vehicle.getVehicleTypeEntity().getWeightLimitTon().multiply(BigDecimal.valueOf(1000)) : null
                    : null)
                .build())
            .orderInfo(OffRouteEventDetailResponse.OrderInfo.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalContractAmount(contractAmount)
                .totalDeclaredValueOfTrip(totalDeclaredValue)
                .senderName(order.getSender() != null ? 
                    order.getSender().getRepresentativeName() : null)
                .senderPhone(order.getSender() != null ? 
                    order.getSender().getRepresentativePhone() : null)
                .senderCompanyName(order.getSender() != null ? 
                    order.getSender().getCompanyName() : null)
                .senderAddress(pickupAddress)
                .senderProvince(order.getPickupAddress() != null ? 
                    order.getPickupAddress().getProvince() : null)
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverIdentity(order.getReceiverIdentity())
                .receiverAddress(deliveryAddress)
                .receiverProvince(order.getDeliveryAddress() != null ? 
                    order.getDeliveryAddress().getProvince() : null)
                .build())
            .packages(packages)
            .build();
    }
}
