package capstone_project.controller.pricing;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.CategoryResponse;
import capstone_project.dtos.response.pricing.FullSizeRuleResponse;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.service.services.order.category.CategoryService;
import capstone_project.service.services.pricing.SizeRuleService;
import capstone_project.service.services.vehicle.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public API endpoints for pricing information
 * These endpoints are accessible without authentication for customer/guest users
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public/pricing")
@RequiredArgsConstructor
public class PublicPricingController {

    private final SizeRuleService sizeRuleService;
    private final CategoryService categoryService;
    private final VehicleTypeService vehicleTypeService;

    /**
     * Get all active size rules with basing prices for public display
     */
    @GetMapping("/size-rules")
    public ResponseEntity<ApiResponse<List<FullSizeRuleResponse>>> getPublicSizeRules() {
        log.info("Public API: Fetching all active size rules");
        final var allRules = sizeRuleService.getAllFullsizeRules();
        
        // Filter only ACTIVE rules for public display
        final var activeRules = allRules.stream()
                .filter(rule -> "ACTIVE".equalsIgnoreCase(rule.status()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.ok(activeRules));
    }

    /**
     * Get all categories for public display
     * Note: CategoryResponse doesn't have status field, so we return all categories
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getPublicCategories() {
        log.info("Public API: Fetching all categories");
        final var allCategories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.ok(allCategories));
    }

    /**
     * Get all vehicle types for public display
     * Note: VehicleTypeResponse doesn't have status field, so we return all vehicle types
     */
    @GetMapping("/vehicle-types")
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> getPublicVehicleTypes() {
        log.info("Public API: Fetching all vehicle types");
        final var allVehicleTypes = vehicleTypeService.getAllVehicleTypes();
        return ResponseEntity.ok(ApiResponse.ok(allVehicleTypes));
    }

    /**
     * Get insurance policy information for public display
     */
    @GetMapping("/insurance-info")
    public ResponseEntity<ApiResponse<InsuranceInfoResponse>> getInsuranceInfo() {
        log.info("Public API: Fetching insurance information");
        
        // Return static insurance policy information
        InsuranceInfoResponse insuranceInfo = new InsuranceInfoResponse(
                100000000L, // Maximum coverage: 100 million VND
                0.5, // Insurance rate: 0.5%
                "7-14 ngày làm việc", // Processing time
                List.of(
                        "Hư hỏng hàng hóa do tai nạn giao thông",
                        "Mất mát hàng hóa trong quá trình vận chuyển",
                        "Hư hỏng do thiên tai, hỏa hoạn",
                        "Hư hỏng do bốc xếp hàng hóa"
                ),
                List.of(
                        "Hàng hóa không khai báo hoặc khai báo sai",
                        "Hàng hóa cấm vận chuyển theo quy định pháp luật",
                        "Hàng hóa hư hỏng do đóng gói không đúng quy cách",
                        "Thiệt hại do chiến tranh, khủng bố"
                )
        );
        
        return ResponseEntity.ok(ApiResponse.ok(insuranceInfo));
    }

    /**
     * Get transportation policies for public display
     */
    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<TransportationPoliciesResponse>> getTransportationPolicies() {
        log.info("Public API: Fetching transportation policies");
        
        TransportationPoliciesResponse policies = new TransportationPoliciesResponse(
                List.of(
                        new PolicyItem("Hủy trước khi xác nhận", "Miễn phí", "cancel"),
                        new PolicyItem("Hủy sau khi xác nhận, trước khi lấy hàng", "10% giá trị đơn hàng", "cancel"),
                        new PolicyItem("Hủy sau khi lấy hàng", "30-50% giá trị đơn hàng", "cancel")
                ),
                List.of(
                        new PolicyItem("Sai sót do vận chuyển", "Hoàn tiền 100%", "refund"),
                        new PolicyItem("Hàng hư hỏng có bảo hiểm", "Theo giá trị bảo hiểm", "refund"),
                        new PolicyItem("Giao hàng chậm trễ", "Giảm 10-20% phí vận chuyển", "refund")
                ),
                List.of(
                        new PolicyItem("Hàng thông thường", "Giá cơ bản", "category"),
                        new PolicyItem("Hàng cồng kềnh", "Phụ thu 20-30%", "category"),
                        new PolicyItem("Hàng nguy hiểm", "Phụ thu 50-100%, yêu cầu giấy phép", "category")
                )
        );
        
        return ResponseEntity.ok(ApiResponse.ok(policies));
    }

    // Inner classes for response DTOs
    public record InsuranceInfoResponse(
            Long maxCoverage,
            Double insuranceRate,
            String processingTime,
            List<String> coveredCases,
            List<String> excludedCases
    ) {}

    public record TransportationPoliciesResponse(
            List<PolicyItem> cancellationPolicies,
            List<PolicyItem> refundPolicies,
            List<PolicyItem> categoryPolicies
    ) {}

    public record PolicyItem(
            String title,
            String description,
            String type
    ) {}
}
