package capstone_project.controller.issue;

import capstone_project.dtos.request.issue.*;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.service.services.issue.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${issue.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class IssuesController {
    private final IssueService issueService;

    // Get Issue by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> getIssueById(@PathVariable("id") UUID id) {
        final var result = issueService.getBasicIssue(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issue by Vehicle Assignment
    @GetMapping("/vehicle/{vehicleAssignmentId}")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> getByVehicleAssignment(
            @PathVariable("vehicleAssignmentId") UUID vehicleAssignmentId) {
        final var result = issueService.getByVehicleAssignment(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issues by Staff
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getByStaff(@PathVariable("staffId") UUID staffId) {
        final var result = issueService.getByStaffId(staffId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issues by active status (OPEN)
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getActiveIssues() {
        final var result = issueService.getByActiveStatus();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issue by IssueType ID
    @GetMapping("/type/{issueTypeId}")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getByIssueType(@PathVariable("issueTypeId") UUID issueTypeId) {
        final var result = issueService.getIssueType(issueTypeId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Create Issue
    @PostMapping("")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> createIssue(@RequestBody CreateBasicIssueRequest request) {
        final var result = issueService.createIssue(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Update Issue
    @PutMapping("")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> updateIssue(@RequestBody UpdateBasicIssueRequest request) {
        final var result = issueService.updateIssue(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/assign-staff")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> assignStaffForIssue(@RequestBody AssignStaffForIssueRequest request) {
        final var result = issueService.updateStaffForIssue(request.staffId(),request.issueId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/inactive")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getInactiveIssues() {
        final var result = issueService.getInactiveStatus();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getAll() {
        final var result = issueService.getAllIssues();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Resolve Issue (restore order detail statuses)
    @PutMapping("/{issueId}/resolve")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> resolveIssue(@PathVariable("issueId") UUID issueId) {
        final var result = issueService.resolveIssue(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Update Issue Status (for simple status updates like PENALTY issues)
    @PutMapping("/{issueId}/status")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> updateIssueStatus(
            @PathVariable("issueId") UUID issueId,
            @RequestParam("status") String status) {
        final var result = issueService.updateIssueStatus(issueId, status);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==================== SEAL REPLACEMENT ENDPOINTS ====================

    // Driver reports seal removal issue
    @PostMapping(value = "/seal-removal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> reportSealIssue(
            @RequestParam("vehicleAssignmentId") UUID vehicleAssignmentId,
            @RequestParam("issueTypeId") UUID issueTypeId,
            @RequestParam("sealId") UUID sealId,
            @RequestParam("description") String description,
            @RequestParam(value = "locationLatitude", required = false) Double locationLatitude,
            @RequestParam(value = "locationLongitude", required = false) Double locationLongitude,
            @RequestParam("sealRemovalImage") MultipartFile sealRemovalImage) {
        
        ReportSealIssueRequest request = new ReportSealIssueRequest(
                vehicleAssignmentId,
                issueTypeId,
                sealId,
                description,
                locationLatitude,
                locationLongitude,
                sealRemovalImage
        );
        
        final var result = issueService.reportSealIssue(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Staff assigns new seal to replace removed seal
    @PutMapping("/seal-replacement/assign")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> assignNewSeal(@RequestBody AssignNewSealRequest request) {
        final var result = issueService.assignNewSeal(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Driver confirms new seal attachment
    @PutMapping("/seal-replacement/confirm")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> confirmNewSeal(@RequestBody ConfirmNewSealRequest request) {
        final var result = issueService.confirmNewSeal(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get IN_USE seal for a vehicle assignment (for driver to report seal removal)
    @GetMapping("/vehicle-assignment/{vehicleAssignmentId}/in-use-seal")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.order.seal.GetSealResponse>> getInUseSeal(
            @PathVariable("vehicleAssignmentId") UUID vehicleAssignmentId) {
        final var result = issueService.getInUseSealByVehicleAssignment(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    // Get available ACTIVE seals for a vehicle assignment (for staff to choose)
    @GetMapping("/vehicle-assignment/{vehicleAssignmentId}/active-seals")
    public ResponseEntity<ApiResponse<List<capstone_project.dtos.response.order.seal.GetSealResponse>>> getActiveSeals(
            @PathVariable("vehicleAssignmentId") UUID vehicleAssignmentId) {
        final var result = issueService.getActiveSealsByVehicleAssignment(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    // Get pending seal replacements for a vehicle assignment (for driver to confirm)
    @GetMapping("/vehicle-assignment/{vehicleAssignmentId}/pending-seal-replacements")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getPendingSealReplacements(
            @PathVariable("vehicleAssignmentId") UUID vehicleAssignmentId) {
        final var result = issueService.getPendingSealReplacementsByVehicleAssignment(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==================== DAMAGE REPORTING ENDPOINTS ====================

    // Driver reports damaged goods issue
    @PostMapping(value = "/damage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> reportDamageIssue(
            @RequestParam("vehicleAssignmentId") UUID vehicleAssignmentId,
            @RequestParam("issueTypeId") UUID issueTypeId,
            @RequestParam("orderDetailIds") List<String> orderDetailIds,
            @RequestParam("description") String description,
            @RequestParam(value = "locationLatitude", required = false) Double locationLatitude,
            @RequestParam(value = "locationLongitude", required = false) Double locationLongitude,
            @RequestParam("damageImages") List<MultipartFile> damageImages) {

        ReportDamageIssueRequest request = new ReportDamageIssueRequest(
                vehicleAssignmentId,
                issueTypeId,
                orderDetailIds,
                description,
                locationLatitude,
                locationLongitude,
                damageImages
        );

        final var result = issueService.reportDamageIssue(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==================== PENALTY REPORTING ENDPOINTS ====================

    // Driver reports traffic penalty violation issue
    @PostMapping(value = "/penalty", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> reportPenaltyIssue(
            @RequestParam("vehicleAssignmentId") UUID vehicleAssignmentId,
            @RequestParam("issueTypeId") UUID issueTypeId,
            @RequestParam("violationType") String violationType,
            @RequestParam(value = "locationLatitude", required = false) Double locationLatitude,
            @RequestParam(value = "locationLongitude", required = false) Double locationLongitude,
            @RequestParam("trafficViolationRecordImage") MultipartFile trafficViolationRecordImage) {

        final var result = issueService.reportPenaltyIssue(
                vehicleAssignmentId,
                issueTypeId,
                violationType,
                trafficViolationRecordImage,
                locationLatitude,
                locationLongitude
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==================== ORDER_REJECTION FLOW ENDPOINTS ====================

    // Driver reports order rejection by recipient
    // Driver simply selects packages to return, server auto-fills issue type and description
    @PostMapping("/order-rejection")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> reportOrderRejection(
            @RequestBody capstone_project.dtos.request.issue.ReportOrderRejectionRequest request) {
        final var result = issueService.reportOrderRejection(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Calculate return shipping fee for ORDER_REJECTION issue
    @GetMapping("/order-rejection/{issueId}/return-fee")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.ReturnShippingFeeResponse>> calculateReturnShippingFee(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.calculateReturnShippingFee(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    // Calculate return shipping fee with custom distance for ORDER_REJECTION issue
    @GetMapping("/order-rejection/{issueId}/return-fee-with-distance")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.ReturnShippingFeeResponse>> calculateReturnShippingFeeWithDistance(
            @PathVariable("issueId") UUID issueId,
            @RequestParam("distanceKm") java.math.BigDecimal distanceKm) {
        System.out.println("🔍 Controller received distance parameter: " + distanceKm + " km for issue: " + issueId);
        final var result = issueService.calculateReturnShippingFee(issueId, distanceKm);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Staff processes ORDER_REJECTION: create transaction and route
    @PostMapping("/order-rejection/process")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.OrderRejectionDetailResponse>> processOrderRejection(
            @RequestBody capstone_project.dtos.request.issue.ProcessOrderRejectionRequest request) {
        final var result = issueService.processOrderRejection(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get ORDER_REJECTION issue detail
    @GetMapping("/order-rejection/{issueId}/detail")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.OrderRejectionDetailResponse>> getOrderRejectionDetail(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.getOrderRejectionDetail(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Driver confirms return delivery at pickup location
    @PostMapping(value = "/order-rejection/confirm-return", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> confirmReturnDelivery(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("issueId") String issueId) throws IOException {
        final var result = issueService.confirmReturnDelivery(files, UUID.fromString(issueId));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    // Customer creates return shipping payment transaction
    @PostMapping("/{issueId}/create-return-payment")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.order.transaction.TransactionResponse>> createReturnPayment(
            @PathVariable("issueId") UUID issueId) {
        System.out.println("💳 [IssuesController] Creating return payment for issue: " + issueId);
        System.out.println("💳 [IssuesController] Authentication: " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
        final var result = issueService.createReturnPaymentTransaction(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result, "Đã tạo giao dịch thanh toán trả hàng"));
    }

    // ==================== REROUTE FLOW ENDPOINTS ====================

    // Driver reports reroute issue when encountering problem on journey segment
    @PostMapping(value = "/reroute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> reportRerouteIssue(
            @RequestParam("vehicleAssignmentId") UUID vehicleAssignmentId,
            @RequestParam("issueTypeId") UUID issueTypeId,
            @RequestParam("affectedSegmentId") UUID affectedSegmentId,
            @RequestParam("description") String description,
            @RequestParam(value = "locationLatitude", required = false) java.math.BigDecimal locationLatitude,
            @RequestParam(value = "locationLongitude", required = false) java.math.BigDecimal locationLongitude,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) throws IOException {

        capstone_project.dtos.request.issue.ReportRerouteRequest request = 
                new capstone_project.dtos.request.issue.ReportRerouteRequest(
                        description,
                        vehicleAssignmentId,
                        issueTypeId,
                        affectedSegmentId,
                        locationLatitude,
                        locationLongitude
                );

        final var result = issueService.reportRerouteIssue(request, images);
        return ResponseEntity.ok(ApiResponse.ok(result, "Đã báo cáo sự cố tái định tuyến"));
    }

    // Staff processes REROUTE issue: create new journey with alternative route
    @PostMapping("/reroute/process")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.RerouteDetailResponse>> processReroute(
            @RequestBody capstone_project.dtos.request.issue.ProcessRerouteRequest request) {
        final var result = issueService.processReroute(request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Đã xử lý tái định tuyến thành công"));
    }

    // Get REROUTE issue detail
    @GetMapping("/reroute/{issueId}/detail")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.RerouteDetailResponse>> getRerouteDetail(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.getRerouteDetail(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    // Get suggested alternative routes for REROUTE issue
    @GetMapping("/reroute/{issueId}/suggested-routes")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.vietmap.VietmapRouteV3Response>> getSuggestedRoutesForReroute(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.getSuggestedRoutesForReroute(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ===== OFF_ROUTE_RUNAWAY endpoints =====
    
    // Get OFF_ROUTE_RUNAWAY issue detail with packages
    @GetMapping("/off-route-runaway/{issueId}/detail")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.OffRouteRunawayDetailResponse>> getOffRouteRunawayDetail(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.getOffRouteRunawayDetail(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    // ===== DAMAGE compensation endpoints =====
    
    /**
     * Update DAMAGE issue compensation information (Staff)
     * Calculates compensation based on insurance policy and saves assessment data.
     */
    @PutMapping("/damage/compensation")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> updateDamageCompensation(
            @RequestBody @jakarta.validation.Valid capstone_project.dtos.request.issue.UpdateDamageCompensationRequest request) {
        final var result = issueService.updateDamageCompensation(request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Đã cập nhật thông tin bồi thường"));
    }
    
    /**
     * Get DAMAGE issue compensation details including policy information
     */
    @GetMapping("/damage/{issueId}/compensation")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.DamageCompensationResponse>> getDamageCompensationDetail(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.getDamageCompensationDetail(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
