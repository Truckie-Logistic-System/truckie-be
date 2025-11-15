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
    @PreAuthorize("hasAnyRole('STAFF', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.ReturnShippingFeeResponse>> calculateReturnShippingFee(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.calculateReturnShippingFee(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    // Calculate return shipping fee with custom distance for ORDER_REJECTION issue
    @GetMapping("/order-rejection/{issueId}/return-fee-with-distance")
    @PreAuthorize("hasAnyRole('STAFF', 'CUSTOMER')")
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
    @PreAuthorize("hasAnyRole('STAFF', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.issue.OrderRejectionDetailResponse>> getOrderRejectionDetail(
            @PathVariable("issueId") UUID issueId) {
        final var result = issueService.getOrderRejectionDetail(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Driver confirms return delivery at pickup location
    @PutMapping("/order-rejection/confirm-return")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> confirmReturnDelivery(
            @RequestBody capstone_project.dtos.request.issue.ConfirmReturnDeliveryRequest request) {
        final var result = issueService.confirmReturnDelivery(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
