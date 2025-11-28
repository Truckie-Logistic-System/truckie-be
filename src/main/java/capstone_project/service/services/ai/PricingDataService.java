package capstone_project.service.services.ai;

import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.repositories.order.order.CategoryPricingDetailRepository;
import capstone_project.repository.repositories.order.order.CategoryRepository;
import capstone_project.repository.repositories.pricing.BasingPriceRepository;
import capstone_project.repository.repositories.pricing.DistanceRuleRepository;
import capstone_project.repository.repositories.pricing.SizeRuleRepository;
import capstone_project.repository.repositories.vehicle.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to fetch and format real-time pricing data from database
 * This ensures AI chatbot always has up-to-date pricing information
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingDataService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryPricingDetailRepository categoryPricingDetailRepository;
    private final DistanceRuleRepository distanceRuleRepository;
    private final SizeRuleRepository sizeRuleRepository;
    private final BasingPriceRepository basingPriceRepository;
    private final capstone_project.service.services.pricing.UnifiedPricingService unifiedPricingService;

    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    /**
     * Generate comprehensive pricing knowledge base from database
     */
    public String generatePricingKnowledgeBase() {
        StringBuilder kb = new StringBuilder();

        kb.append("# BẢNG GIÁ VẬN CHUYỂN (Real-time Data from System)\n\n");
        kb.append("⚠️ **Dữ liệu sau đây được lấy trực tiếp từ hệ thống, đảm bảo tính chính xác tuyệt đối.**\n\n");

        // 1. Vehicle Types with Size Rules
        kb.append("## 🚛 Danh Sách Loại Xe (8 loại)\n\n");
        List<VehicleTypeEntity> vehicles = vehicleTypeRepository.findAll();
        List<SizeRuleEntity> sizeRules = sizeRuleRepository.findAll();
        
        // Map vehicle types to their size rules
        Map<String, SizeRuleEntity> sizeRuleMap = sizeRules.stream()
                .filter(sr -> sr.getVehicleTypeEntity() != null)
                .collect(Collectors.toMap(sr -> sr.getVehicleTypeEntity().getId().toString(), sr -> sr));

        for (VehicleTypeEntity vehicle : vehicles) {
            String vehicleId = vehicle.getId().toString();
            SizeRuleEntity sizeRule = sizeRuleMap.get(vehicleId);
            
            kb.append(String.format("### **%s** (%s)\n",
                    vehicle.getVehicleTypeName(),
                    vehicle.getDescription() != null ? vehicle.getDescription() : "Xe tải"
            ));
            
            // Use vehicle weight limit if available, otherwise use size rule
            if (vehicle.getWeightLimitTon() != null) {
                kb.append(String.format("- **Trọng tải tối đa**: %.2f tấn (%.0f kg)\n",
                        vehicle.getWeightLimitTon(),
                        vehicle.getWeightLimitTon().multiply(BigDecimal.valueOf(1000))
                ));
            } else if (sizeRule != null && sizeRule.getMaxWeight() != null) {
                kb.append(String.format("- **Trọng tải tối đa**: %.2f tấn (%.0f kg)\n",
                        sizeRule.getMaxWeight(),
                        sizeRule.getMaxWeight().multiply(BigDecimal.valueOf(1000))
                ));
            }
            
            if (sizeRule != null) {
                kb.append(String.format("- **Kích thước**: Dài %.2fm × Rộng %.2fm × Cao %.2fm\n",
                        sizeRule.getMaxLength(),
                        sizeRule.getMaxWidth(),
                        sizeRule.getMaxHeight()
                ));
                // Calculate volume if dimensions available
                if (sizeRule.getMaxLength() != null && sizeRule.getMaxWidth() != null && sizeRule.getMaxHeight() != null) {
                    BigDecimal volume = sizeRule.getMaxLength()
                            .multiply(sizeRule.getMaxWidth())
                            .multiply(sizeRule.getMaxHeight());
                    kb.append(String.format("- **Thể tích tối đa**: %.2f m³\n", volume));
                }
            }
            
            // Add fuel consumption if available
            if (vehicle.getAverageFuelConsumptionLPer100km() != null) {
                kb.append(String.format("- **Mức tiêu thụ nhiên liệu**: %.3f lít/100km\n",
                        vehicle.getAverageFuelConsumptionLPer100km()
                ));
            }
            
            kb.append("\n");
        }

        // 2. Vehicle Comparison Guide
        kb.append("## � Hướng Dẫn So Sánh Loại Xe\n\n");
        kb.append("**Khi chọn xe, hãy cân nhắc:**\n");
        kb.append("- **Trọng lượng hàng hóa**: Chọn xe có tải trọng lớn hơn hàng hóa ít nhất 10-20%\n");
        kb.append("- **Kích thước hàng hóa**: Đảm bảo hàng vừa trong thùng xe\n");
        kb.append("- **Quãng đường**: Xe lớn hơn phù hợp cho đường dài, xe nhỏ hơn linh hoạt trong nội thành\n");
        kb.append("- **Chi phí**: Xe lớn có giá cao hơn nhưng có thể chở nhiều hơn trong 1 chuyến\n\n");
        
        // 3. Important Notes
        kb.append("## ⚠️ Lưu Ý Quan Trọng\n\n");
        kb.append("1. **Thông tin xe** được lấy trực tiếp từ hệ thống, đảm bảo tính chính xác\n");
        kb.append("2. **Giá vận chuyển** được tính dựa trên loại xe, khoảng cách và loại hàng hóa\n");
        kb.append("3. **Để báo giá chính xác**, vui lòng tạo đơn hàng hoặc liên hệ hotline\n");
        kb.append("4. **Nhân viên sẽ tư vấn** loại xe phù hợp nhất cho nhu cầu của bạn\n\n");

        log.info("✅ Generated vehicle knowledge base with {} vehicles", vehicles.size());

        return kb.toString();
    }

    /**
     * Generate example calculation with real data using UnifiedPricingService
     * @param weightTons Trọng lượng hàng hóa (tấn)
     * @param distanceKm Khoảng cách (km)
     * @param categoryName Tên loại hàng hóa
     * @return Ví dụ tính phí chi tiết
     */
    public String generatePricingExample(double weightTons, double distanceKm, String categoryName) {
        StringBuilder example = new StringBuilder();
        
        try {
            // Find suitable vehicle (SizeRuleEntity)
            SizeRuleEntity sizeRule = findSuitableVehicle(weightTons);
            if (sizeRule == null) {
                return "⚠️ Không tìm thấy loại xe phù hợp cho trọng lượng này.";
            }

            // Get vehicle type name
            String vehicleTypeName = sizeRule.getVehicleTypeEntity() != null 
                    ? sizeRule.getVehicleTypeEntity().getVehicleTypeName() 
                    : sizeRule.getSizeRuleName();

            // Find category
            CategoryEntity category = categoryRepository.findAll().stream()
                    .filter(c -> c.getCategoryName().name().equalsIgnoreCase(categoryName))
                    .findFirst()
                    .orElse(categoryRepository.findAll().get(0)); // Default to first category

            example.append(String.format("## Ví Dụ Tính Phí: %.1f tấn, %.0f km, %s\n\n",
                    weightTons, distanceKm, category.getCategoryName().name()));

            example.append(String.format("1. **Chọn xe**: %s (tải trọng tối đa %.2f tấn)\n",
                    vehicleTypeName,
                    sizeRule.getMaxWeight()
            ));

            // Use UnifiedPricingService for consistent calculation
            BigDecimal distance = BigDecimal.valueOf(distanceKm);
            var pricingResult = unifiedPricingService.calculatePrice(
                    sizeRule.getId(), 
                    distance, 
                    1,  // 1 vehicle for example
                    category.getId()
            );

            if (!pricingResult.isSuccess()) {
                return "⚠️ Không thể tính giá. Vui lòng liên hệ nhân viên.";
            }

            // Display tier breakdown
            example.append("\n2. **Tính theo từng đoạn đường**:\n");
            for (var tier : pricingResult.getTierResults()) {
                example.append(String.format("   - %s: %.1f km × %s VND/km = %s VND\n",
                        tier.getDistanceRange(),
                        tier.getAppliedKm().doubleValue(),
                        VND_FORMAT.format(tier.getUnitPrice()),
                        VND_FORMAT.format(tier.getSubtotal())
                ));
            }
            example.append(String.format("   - **Tổng cơ bản**: %s VND\n\n",
                    VND_FORMAT.format(pricingResult.getBasePriceForOneVehicle())));

            // Display category adjustment
            CategoryPricingDetailEntity categoryPrice = categoryPricingDetailRepository.findAll().stream()
                    .filter(cp -> cp.getCategory().getId().equals(category.getId()))
                    .findFirst()
                    .orElse(null);

            if (categoryPrice != null) {
                example.append(String.format("3. **Điều chỉnh loại hàng** (%s):\n", category.getCategoryName().name()));
                example.append(String.format("   - Hệ số nhân: ×%.2f\n",
                        categoryPrice.getPriceMultiplier()
                ));
                if (categoryPrice.getExtraFee() != null && categoryPrice.getExtraFee().compareTo(BigDecimal.ZERO) > 0) {
                    example.append(String.format("   - Phụ phí: +%s VND\n",
                            VND_FORMAT.format(categoryPrice.getExtraFee())
                    ));
                }
                example.append(String.format("   - Sau điều chỉnh: %s VND\n\n",
                        VND_FORMAT.format(pricingResult.getAdjustedPriceForOneVehicle())
                ));
            }

            example.append(String.format("4. **Cước vận chuyển (đã làm tròn)**: **%s VND**\n\n",
                    VND_FORMAT.format(pricingResult.getTotalPrice())
            ));

            // Add insurance fee section
            example.append("5. **Phí bảo hiểm hàng hóa (TÙY CHỌN)**:\n");
            example.append("   - Hàng thông thường: 0.08% × Giá trị khai báo × 1.10 (VAT) = **0.088%** giá trị khai báo\n");
            example.append("   - Hàng dễ vỡ/rủi ro cao: 0.15% × Giá trị khai báo × 1.10 (VAT) = **0.165%** giá trị khai báo\n");
            example.append("   - Ví dụ: Hàng trị giá 100 triệu VND:\n");
            example.append("     + Hàng thường: 100,000,000 × 0.088% = **88,000 VND**\n");
            example.append("     + Hàng dễ vỡ: 100,000,000 × 0.165% = **165,000 VND**\n\n");

            example.append("6. **TỔNG CHI PHÍ = Cước vận chuyển + Phí bảo hiểm (nếu có)**\n\n");

            example.append("⚠️ **Lưu ý quan trọng**:\n");
            example.append("   - Giá trên chưa bao gồm VAT (10%) cho cước vận chuyển\n");
            example.append("   - Bảo hiểm là TÙY CHỌN, giúp bảo vệ quyền lợi khi xảy ra sự cố\n");
            example.append("   - Nếu KHÔNG mua bảo hiểm → Bồi thường tối đa 10 × Cước phí (Điều 546 Luật TM 2005)\n");
            example.append("📞 Để biết giá chính xác, vui lòng tạo đơn hàng hoặc liên hệ hotline.\n");

        } catch (Exception e) {
            log.error("Error generating pricing example", e);
            return "⚠️ Không thể tính ví dụ. Vui lòng liên hệ nhân viên để được hỗ trợ.";
        }

        return example.toString();
    }

    /**
     * Find suitable vehicle (SizeRuleEntity) based on weight
     * @param weightTons Trọng lượng hàng hóa (tấn)
     * @return SizeRuleEntity phù hợp hoặc null nếu không tìm thấy
     */
    private SizeRuleEntity findSuitableVehicle(double weightTons) {
        List<SizeRuleEntity> rules = sizeRuleRepository.findAll();
        
        // Filter active rules and sort by weight capacity
        rules = rules.stream()
                .filter(r -> r.getMaxWeight() != null)
                .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()) || r.getStatus() == null)
                .sorted(Comparator.comparing(SizeRuleEntity::getMaxWeight))
                .toList();

        // Find smallest vehicle that can carry the weight
        for (SizeRuleEntity rule : rules) {
            if (rule.getMaxWeight().doubleValue() >= weightTons) {
                return rule;
            }
        }
        
        // If no exact match, return largest vehicle
        return rules.isEmpty() ? null : rules.get(rules.size() - 1);
    }

    /**
     * Generate pricing comparison between different vehicle types for same route
     * @param weightTons Trọng lượng hàng hóa
     * @param distanceKm Khoảng cách
     * @param categoryName Loại hàng hóa
     * @return So sánh giá giữa các loại xe
     */
    public String generateVehicleComparison(double weightTons, double distanceKm, String categoryName) {
        StringBuilder comparison = new StringBuilder();
        
        try {
            // Find category
            CategoryEntity category = categoryRepository.findAll().stream()
                    .filter(c -> c.getCategoryName().name().equalsIgnoreCase(categoryName))
                    .findFirst()
                    .orElse(categoryRepository.findAll().get(0));

            comparison.append(String.format("## So Sánh Giá Xe: %.1f tấn, %.0f km, %s\n\n",
                    weightTons, distanceKm, category.getCategoryName().name()));

            // Get all suitable vehicles
            List<SizeRuleEntity> suitableVehicles = sizeRuleRepository.findAll().stream()
                    .filter(r -> r.getMaxWeight() != null)
                    .filter(r -> r.getMaxWeight().doubleValue() >= weightTons)
                    .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()) || r.getStatus() == null)
                    .sorted(Comparator.comparing(SizeRuleEntity::getMaxWeight))
                    .limit(3)  // Top 3 suitable vehicles
                    .toList();

            if (suitableVehicles.isEmpty()) {
                return "⚠️ Không tìm thấy xe phù hợp cho trọng lượng này.";
            }

            comparison.append("| Loại Xe | Tải Trọng | Giá Ước Tính |\n");
            comparison.append("|---------|-----------|--------------|\n");

            for (SizeRuleEntity sizeRule : suitableVehicles) {
                String vehicleName = sizeRule.getVehicleTypeEntity() != null 
                        ? sizeRule.getVehicleTypeEntity().getVehicleTypeName() 
                        : sizeRule.getSizeRuleName();

                var pricingResult = unifiedPricingService.calculatePrice(
                        sizeRule.getId(),
                        BigDecimal.valueOf(distanceKm),
                        1,
                        category.getId()
                );

                if (pricingResult.isSuccess()) {
                    comparison.append(String.format("| %s | %.2f tấn | %s VND |\n",
                            vehicleName,
                            sizeRule.getMaxWeight(),
                            VND_FORMAT.format(pricingResult.getTotalPrice())
                    ));
                }
            }

            comparison.append("\n⚠️ **Lưu ý**: Giá trên chỉ mang tính chất tham khảo.\n");
            comparison.append("📞 Để được tư vấn chi tiết, vui lòng liên hệ hotline hoặc tạo đơn hàng.\n");

        } catch (Exception e) {
            log.error("Error generating vehicle comparison", e);
            return "⚠️ Không thể so sánh giá xe. Vui lòng liên hệ nhân viên.";
        }

        return comparison.toString();
    }
}
