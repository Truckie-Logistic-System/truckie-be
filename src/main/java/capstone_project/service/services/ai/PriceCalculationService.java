package capstone_project.service.services.ai;

import capstone_project.common.enums.CategoryName;
import capstone_project.common.utils.BinPacker;
import capstone_project.dtos.request.chat.PriceEstimateRequest;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import capstone_project.repository.entityServices.order.order.CategoryPricingDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.entityServices.pricing.DistanceRuleEntityService;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import capstone_project.service.services.pricing.UnifiedPricingService;
import capstone_project.service.services.setting.CarrierSettingService;
import capstone_project.dtos.response.setting.CarrierSettingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCalculationService {

    private final SizeRuleEntityService sizeRuleService;
    private final DistanceRuleEntityService distanceRuleService;
    private final BasingPriceEntityService basingPriceService;
    private final CategoryEntityService categoryEntityService;
    private final CategoryPricingDetailEntityService categoryPricingDetailService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final UnifiedPricingService unifiedPricingService;

    /**
     * Tính giá vận chuyển dựa trên weight, distance
     * Trả về explanation chi tiết - Sử dụng UnifiedPricingService để đảm bảo tính nhất quán
     */
    public PriceEstimateResult estimatePrice(PriceEstimateRequest request) {
        try {
            log.info("🧮 AI Chatbot calculating price: weight={} kg, distance={} km", request.weight(), request.distance());

            // 1. Chọn loại xe phù hợp
            SizeRuleEntity selectedVehicle = selectVehicleByWeight(request.weight());
            if (selectedVehicle == null) {
                return PriceEstimateResult.error("Trọng lượng vượt quá khả năng chở của hệ thống (max 10 tấn)");
            }

            // 1a. Kiểm tra nếu cần nhiều xe
            int numOfVehiclesNeeded = calculateNumberOfVehicles(request.weight(), selectedVehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000)));
            boolean needMultipleVehicles = numOfVehiclesNeeded > 1;

            // 2. Lấy category ID từ category name
            UUID categoryId = null;
            if (request.categoryName() != null && !request.categoryName().equals("Hàng thông thường")) {
                // Convert String to CategoryName enum for repository lookup
                CategoryName categoryNameEnum = CategoryName.fromString(request.categoryName());
                CategoryEntity category = categoryEntityService.findByCategoryName(categoryNameEnum).orElse(null);
                if (category != null) {
                    categoryId = category.getId();
                } else {
                    log.warn("⚠️ Category not found: {}", request.categoryName());
                }
            }

            // 3. Sử dụng UnifiedPricingService để tính giá chính xác
            UnifiedPricingService.UnifiedPriceResult pricingResult = unifiedPricingService.calculatePrice(
                    selectedVehicle.getId(),
                    request.distance(),
                    1, // Calculate for 1 vehicle first, then multiply
                    categoryId
            );

            if (!pricingResult.isSuccess()) {
                return PriceEstimateResult.error("Lỗi tính giá: " + pricingResult.getErrorMessage());
            }

            BigDecimal priceForOneVehicle = pricingResult.getAdjustedPriceForOneVehicle();
            BigDecimal totalPrice = needMultipleVehicles 
                    ? priceForOneVehicle.multiply(BigDecimal.valueOf(numOfVehiclesNeeded))
                    : priceForOneVehicle;

            // 4. Build breakdown chi tiết cho AI chatbot
            StringBuilder breakdown = buildDetailedBreakdown(
                    selectedVehicle, 
                    request, 
                    numOfVehiclesNeeded, 
                    needMultipleVehicles,
                    priceForOneVehicle,
                    totalPrice,
                    categoryId
            );

            log.info("✅ AI Chatbot pricing completed: {} VND (vehicles: {})", 
                    totalPrice, needMultipleVehicles ? numOfVehiclesNeeded + "x" : "1");

            return PriceEstimateResult.success(
                    totalPrice.doubleValue(),
                    selectedVehicle.getSizeRuleName(),
                    breakdown.toString()
            );

        } catch (Exception e) {
            log.error("❌ Error calculating price in AI chatbot", e);
            return PriceEstimateResult.error("Không thể tính giá lúc này. Vui lòng thử lại sau.");
        }
    }

    /**
     * Calculate price with dimensions for accurate vehicle allocation using BinPacker
     * 
     * @param weight Total weight in kg
     * @param distance Distance in km  
     * @param packageInfo List of package dimensions and weights
     * @param categoryName Category name
     * @return Accurate pricing result with realistic vehicle allocation
     */
    public AllVehiclePriceResult estimatePriceWithDimensions(BigDecimal weight, BigDecimal distance,
                                                           List<PackageInfo> packageInfo, String categoryName) {
        try {
            log.info("🧮 AI Chatbot calculating ACCURATE price with dimensions: weight={} kg, distance={} km, packages={}", 
                    weight, distance, packageInfo.size());

            // 1. Create fake OrderDetailEntity list from package info
            List<OrderDetailEntity> fakeDetails = createFakeOrderDetails(packageInfo);

            // 2. Get available vehicles for category
            // Convert String to CategoryName enum for repository lookup
            log.info("🔍 DEBUG: Incoming category name: '{}'", categoryName);
            CategoryName categoryNameEnum = CategoryName.fromString(categoryName);
            log.info("🔍 DEBUG: Resolved CategoryName enum: '{}'", categoryNameEnum);
            CategoryEntity category = categoryEntityService.findByCategoryName(categoryNameEnum)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));
            log.info("🔍 DEBUG: Found category entity - ID: '{}', Name: '{}'", category.getId(), category.getCategoryName());
            
            List<SizeRuleEntity> allVehicles = sizeRuleService.findAllByCategoryId(category.getId())
                    .stream()
                    .filter(rule -> "ACTIVE".equals(rule.getStatus()))
                    .sorted(Comparator.comparing(SizeRuleEntity::getMaxWeight))
                    .collect(Collectors.toList());
            log.info("🔍 DEBUG: Found {} size rules for category ID: {}", allVehicles.size(), category.getId());
            if (!allVehicles.isEmpty()) {
                log.info("🔍 DEBUG: Size rules weight range: {} - {} tons", 
                    allVehicles.get(0).getMaxWeight(), 
                    allVehicles.get(allVehicles.size() - 1).getMaxWeight());
            }

            if (allVehicles.isEmpty()) {
                return AllVehiclePriceResult.error("Không tìm thấy loại xe nào cho category: " + categoryName);
            }

            // 3. Use BinPacker for realistic vehicle allocation
            List<BinPacker.ContainerState> containers = BinPacker.pack(fakeDetails, allVehicles);

            // 4. Calculate pricing for each vehicle
            List<AllVehiclePriceResult> results = new ArrayList<>();
            
            for (BinPacker.ContainerState container : containers) {
                if (container.placements.isEmpty()) continue;

                // Calculate price for this vehicle type
                PriceEstimateRequest vehicleRequest = new PriceEstimateRequest(distance, 
                        container.rule.getMaxWeight().multiply(BigDecimal.valueOf(1000)), 
                        null, categoryName);
                
                PriceEstimateResult result = estimatePriceForSpecificVehicle(vehicleRequest, container.rule);
                
                if (result.isSuccess()) {
                    results.add(AllVehiclePriceResult.success(
                            result.getEstimatedPrice(),
                            result.getVehicleType(),
                            result.getBreakdown(),
                            false, // No vehicle recommendation since we use BinPacker
                            container.rule.getMaxWeight().doubleValue(),
                            categoryName
                    ));
                }
            }

            // 5. Return first result (since we're calculating per category)
            if (results.isEmpty()) {
                return AllVehiclePriceResult.error("Không thể tính giá với thông tin kiện hàng cung cấp");
            }

            return results.get(0); // Return first result for this category

        } catch (Exception e) {
            log.error("❌ Error in accurate pricing calculation", e);
            return AllVehiclePriceResult.error("Lỗi khi tính giá chính xác: " + e.getMessage());
        }
    }

    /**
     * Create fake OrderDetailEntity list from package information
     */
    private List<OrderDetailEntity> createFakeOrderDetails(List<PackageInfo> packageInfo) {
        List<OrderDetailEntity> details = new ArrayList<>();
        
        for (int i = 0; i < packageInfo.size(); i++) {
            PackageInfo info = packageInfo.get(i);
            
            OrderDetailEntity detail = new OrderDetailEntity();
            detail.setId(UUID.randomUUID()); // Fake ID
            detail.setWeightTons(info.weight);
            detail.setTrackingCode("PKG-" + (i + 1));
            
            // Create fake OrderSizeEntity
            OrderSizeEntity size = new OrderSizeEntity();
            size.setMaxLength(info.length);
            size.setMaxWidth(info.width);
            size.setMaxHeight(info.height);
            detail.setOrderSizeEntity(size);
            
            details.add(detail);
        }
        
        return details;
    }

    /**
     * Build detailed breakdown for AI chatbot response
     */
    private StringBuilder buildDetailedBreakdown(SizeRuleEntity selectedVehicle, 
                                               PriceEstimateRequest request,
                                               int numOfVehiclesNeeded,
                                               boolean needMultipleVehicles,
                                               BigDecimal priceForOneVehicle,
                                               BigDecimal totalPrice,
                                               UUID categoryId) {
        StringBuilder breakdown = new StringBuilder();

        breakdown.append(String.format("🚛 **Loại xe:** %s (tải trọng: %.1f tấn)\n",
                selectedVehicle.getSizeRuleName(),
                selectedVehicle.getMaxWeight()));

        if (needMultipleVehicles) {
            breakdown.append(String.format("⚠️ **Lưu ý:** Hàng %.1f tấn cần **%d xe %s** để vận chuyển\n",
                    request.weight().divide(BigDecimal.valueOf(1000)).doubleValue(),
                    numOfVehiclesNeeded,
                    selectedVehicle.getSizeRuleName()));
            breakdown.append(String.format("💡 Giá dưới đây là cho **1 xe**. Tổng chi phí = %,d VND × %d xe = **%,d VND**\n\n",
                    priceForOneVehicle.intValue(),
                    numOfVehiclesNeeded,
                    totalPrice.intValue()));
        } else {
            breakdown.append("\n");
        }

        breakdown.append("📊 **Chi tiết tính phí (1 xe):**\n\n");

        // Add distance tier breakdown (using existing logic for display)
        addDistanceTierBreakdown(breakdown, selectedVehicle, request.distance());

        // Add category adjustment details
        if (categoryId != null) {
            addCategoryAdjustmentBreakdown(breakdown, categoryId);
        }

        breakdown.append(String.format("\n💰 **TỔNG ƯỚC TÍNH (1 xe):** %,d VND\n", priceForOneVehicle.intValue()));
        
        if (needMultipleVehicles) {
            breakdown.append(String.format("💰 **TỔNG CHO %d XE:** %,d VND\n", 
                    numOfVehiclesNeeded, 
                    totalPrice.intValue()));
        }

        breakdown.append("\n⚠️ **Lưu ý:** **Giá trên chỉ là tham khảo**. Giá thực tế có thể thay đổi tùy theo:\n");
        breakdown.append("- Điều kiện đường xá thực tế\n");
        breakdown.append("- Thời gian giao hàng (giờ cao điểm, đêm khuya)\n");
        breakdown.append("- Khu vực giao hàng (vùng sâu, vùng xa)\n");
        breakdown.append("- Phụ phí cầu đường, cao tốc\n\n");
        breakdown.append("📞 Liên hệ nhân viên để nhận báo giá chính xác!");

        return breakdown;
    }

    /**
     * Add distance tier breakdown for display purposes
     */
    private void addDistanceTierBreakdown(StringBuilder breakdown, SizeRuleEntity selectedVehicle, BigDecimal distance) {
        List<DistanceRuleEntity> distanceRules = distanceRuleService.findAll()
                .stream()
                .sorted(Comparator.comparing(DistanceRuleEntity::getFromKm))
                .toList();

        BigDecimal remainingDistance = distance;

        for (DistanceRuleEntity distanceRule : distanceRules) {
            if (remainingDistance.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal from = distanceRule.getFromKm();
            BigDecimal to = distanceRule.getToKm();

            BasingPriceEntity basePrice = basingPriceService
                    .findBasingPriceEntityBysizeRuleEntityIdAndDistanceRuleEntityId(
                            selectedVehicle.getId(), distanceRule.getId())
                    .orElse(null);

            if (basePrice == null) {
                continue;
            }

            // Fixed tier (0-4km)
            if (from.compareTo(BigDecimal.ZERO) == 0 && to.compareTo(BigDecimal.valueOf(4)) == 0) {
                breakdown.append(String.format("- **0-4 km (cố định):** %,d VND\n",
                        basePrice.getBasePrice().intValue()));
                remainingDistance = remainingDistance.subtract(to);
            } else {
                // Variable tier
                BigDecimal tierDistance = (to == null) ? remainingDistance : remainingDistance.min(to.subtract(from));
                BigDecimal tierPrice = basePrice.getBasePrice().multiply(tierDistance);

                breakdown.append(String.format("- **%s-%s km:** %.1f km × %,d VND/km = %,d VND\n",
                        from.intValue(),
                        to == null ? from.intValue() + "+" : to.intValue(),
                        tierDistance.doubleValue(),
                        basePrice.getBasePrice().intValue(),
                        tierPrice.intValue()));

                remainingDistance = remainingDistance.subtract(tierDistance);
            }
        }
    }

    /**
     * Add category adjustment breakdown for display purposes
     */
    private void addCategoryAdjustmentBreakdown(StringBuilder breakdown, UUID categoryId) {
        CategoryPricingDetailEntity pricingDetail = categoryPricingDetailService.findByCategoryId(categoryId);
        if (pricingDetail != null) {
            BigDecimal multiplier = pricingDetail.getPriceMultiplier() != null 
                    ? pricingDetail.getPriceMultiplier() 
                    : BigDecimal.ONE;
            BigDecimal extraFee = pricingDetail.getExtraFee() != null 
                    ? pricingDetail.getExtraFee() 
                    : BigDecimal.ZERO;

            CategoryEntity category = categoryEntityService.findEntityById(categoryId).orElse(null);
            if (category != null) {
                breakdown.append(String.format("\n📦 **Điều chỉnh loại hàng:** %s\n", category.getCategoryName().name()));
                breakdown.append(String.format("- Hệ số: %.1fx\n", multiplier.doubleValue()));
                breakdown.append(String.format("- Phụ phí: %,d VND\n", extraFee.intValue()));
            }
        }
    }

    /**
     * Calculate pricing for ALL vehicle types with dimensions (accurate using BinPacker)
     */
    public List<AllVehiclePriceResult> calculateAllVehiclesPriceWithDimensions(BigDecimal weight, BigDecimal distance,
                                                                             String categoryName, List<PackageInfo> packageInfo) {
        try {
            log.info("🧮 Calculating ACCURATE pricing with dimensions for weight={}kg, distance={}km, packages={}", 
                    weight, distance, packageInfo.size());

            List<CategoryEntity> allCategories = categoryEntityService.findAll();
            log.info("📦 Found {} categories in database", allCategories.size());
            
            List<AllVehiclePriceResult> results = new ArrayList<>();
            
            // Calculate pricing for EACH category using BinPacker allocation
            for (CategoryEntity category : allCategories) {
                log.info("🏷️ Calculating for category: {}", category.getCategoryName().name());
                
                AllVehiclePriceResult result = estimatePriceWithDimensions(weight, distance, packageInfo, category.getCategoryName().name());
                
                if (result.isSuccess()) {
                    log.info("✅ Success for {}: Price {} VND", category.getCategoryName().name(), result.getEstimatedPrice());
                    results.add(result);
                } else {
                    log.warn("❌ Failed for {}: {}", category.getCategoryName().name(), result.getErrorMessage());
                    results.add(AllVehiclePriceResult.error(result.getErrorMessage()));
                }
            }
            
            return results;
            
        } catch (Exception e) {
            log.error("❌ Error in calculateAllVehiclesPriceWithDimensions", e);
            return List.of(AllVehiclePriceResult.error("Lỗi khi tính giá với kích thước: " + e.getMessage()));
        }
    }

    /**
     * Chọn xe phù hợp dựa trên trọng lượng (weight in KG, converts to tons for comparison)
     */
    private SizeRuleEntity selectVehicleByWeight(BigDecimal weightInKg) {
        BigDecimal weightInTons = weightInKg.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        List<SizeRuleEntity> vehicles = sizeRuleService.findAll().stream()
                .filter(v -> v.getMaxWeight().compareTo(weightInTons) >= 0)
                .sorted(Comparator.comparing(v -> v.getMaxWeight().subtract(weightInTons)))
                .toList();

        return vehicles.isEmpty() ? null : vehicles.get(0);
    }

    /**
     * Tính số xe cần thiết
     * @param totalWeightInKg Total weight in KG
     * @param vehicleCapacityInKg Vehicle capacity in KG (convert from TONS by multiplying by 1000)
     */
    private int calculateNumberOfVehicles(BigDecimal totalWeightInKg, BigDecimal vehicleCapacityInKg) {
        if (vehicleCapacityInKg.compareTo(BigDecimal.ZERO) <= 0) {
            return 1;
        }
        
        log.info("🔍 DEBUG: calculateNumberOfVehicles - {}kg ÷ {}kg", totalWeightInKg, vehicleCapacityInKg);
        
        BigDecimal numVehicles = totalWeightInKg.divide(vehicleCapacityInKg, 2, RoundingMode.UP);
        log.info("🔍 DEBUG: division result = {} (before Math.ceil)", numVehicles);
        
        // Round up to nearest integer (Math.ceil equivalent)
        int roundedVehicles = (int) Math.ceil(numVehicles.doubleValue());
        log.info("🔍 DEBUG: Math.ceil result = {} vehicles", roundedVehicles);
        
        return roundedVehicles < 1 ? 1 : roundedVehicles;
    }

    /**
     * Tính giá cho TẤT CẢ các loại category với weight và distance cho trước
     * Hỗ trợ multi-vehicle cho trọng lượng lớn (tối đa 50 tấn)
     */
    public List<AllVehiclePriceResult> calculateAllVehiclesPrice(BigDecimal weight, BigDecimal distance, String categoryName) {
        log.info("🔍 calculateAllVehiclesPrice: weight={}kg, distance={}km", weight, distance);
        
        // CRITICAL: Database stores maxWeight in TONS, but input is in KG
        // Convert weight from KG to TONS for comparison
        BigDecimal weightInTons = weight.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        log.info("🔄 Converting weight: {}kg → {}tons for database comparison", weight, weightInTons);
        
        // BR12: Validate weight range (0.5 tons to 50 tons)
        BigDecimal minWeight = BigDecimal.valueOf(0.5);
        BigDecimal maxWeight = BigDecimal.valueOf(50.0);
        
        if (weightInTons.compareTo(minWeight) < 0) {
            return List.of(AllVehiclePriceResult.error(
                    String.format("Trọng lượng tối thiểu là 0.5 tấn (500 kg). " +
                            "Trọng lượng hiện tại: %.1f tấn. " +
                            "Vui lòng nhập trọng lượng từ 0.5 tấn đến 50 tấn.", weightInTons)
            ));
        }
        
        if (weightInTons.compareTo(maxWeight) > 0) {
            return List.of(AllVehiclePriceResult.error(
                    String.format("Trọng lượng vượt quá giới hạn hệ thống (tối đa 50 tấn). " +
                            "Trọng lượng hiện tại: %.1f tấn. " +
                            "Vui lòng nhập trọng lượng từ 0.5 tấn đến 50 tấn.", weightInTons)
            ));
        }
        
        // Check if weight exceeds maximum single vehicle capacity (10 tons)
        BigDecimal maxSingleVehicleCapacity = BigDecimal.valueOf(10.0); // Max in database
        if (weightInTons.compareTo(maxSingleVehicleCapacity) > 0) {
            return calculateMultiVehiclePricing(weight, distance, categoryName, weightInTons);
        }
        
        // Single vehicle logic for normal cases
        SizeRuleEntity bestVehicle = selectVehicleByWeight(weight);
        
        if (bestVehicle == null) {
            log.warn("⚠️ No suitable vehicle found for weight {}kg", weight);
            return List.of(AllVehiclePriceResult.error("Trọng lượng vượt quá khả năng chở của hệ thống"));
        }
        
        log.info("✅ Selected best vehicle: {} (maxWeight: {}tons)", 
                bestVehicle.getSizeRuleName(), bestVehicle.getMaxWeight());
        
        // Get ALL categories from database
        List<CategoryEntity> allCategories = categoryEntityService.findAll();
        log.info("📦 Found {} categories in database", allCategories.size());
        
        List<AllVehiclePriceResult> results = new ArrayList<>();
        
        // Calculate pricing for EACH category with the SAME best vehicle
        for (CategoryEntity category : allCategories) {
            log.info("🏷️ Calculating for category: {}", category.getCategoryName().name());
            
            PriceEstimateRequest request = new PriceEstimateRequest(distance, weight, null, category.getCategoryName().name());
            PriceEstimateResult result = estimatePriceForSpecificVehicle(request, bestVehicle);
            
            if (result.isSuccess()) {
                log.info("✅ Success for {}: Price {} VND", category.getCategoryName().name(), result.getEstimatedPrice());
                
                results.add(AllVehiclePriceResult.success(
                        result.getEstimatedPrice(),
                        result.getVehicleType(),
                        result.getBreakdown(),
                        false, // No vehicle recommendation since we only show one vehicle
                        bestVehicle.getMaxWeight().doubleValue(),
                        category.getCategoryName().name()
                ));
            } else {
                log.warn("❌ Failed for category {}: {}", category.getCategoryName().name(), result.getErrorMessage());
            }
        }
        
        log.info("📈 Returning {} category pricing results", results.size());
        return results;
    }

    /**
     * Tính giá cho multi-vehicle (trọng lượng > 10 tấn, tối đa 50 tấn)
     * BR12 validation already handled in calculateAllVehiclesPrice
     */
    private List<AllVehiclePriceResult> calculateMultiVehiclePricing(BigDecimal weight, BigDecimal distance, 
            String categoryName, BigDecimal weightInTons) {
        
        // Get all available vehicles for optimal allocation
        List<SizeRuleEntity> allVehicles = sizeRuleService.findAll();
        if (allVehicles.isEmpty()) {
            return List.of(AllVehiclePriceResult.error("Không tìm thấy loại xe phù hợp"));
        }
        
        // Find optimal vehicle allocation
        VehicleAllocation optimalAllocation = findOptimalVehicleAllocation(weight, distance, allVehicles);
        
        log.info("🚛 Optimal vehicle allocation: {} tấn → {}", 
                weightInTons, optimalAllocation.getDescription());
        
        // Get all categories
        List<CategoryEntity> allCategories = categoryEntityService.findAll();
        List<AllVehiclePriceResult> results = new ArrayList<>();
        
        // Calculate for each category
        for (CategoryEntity category : allCategories) {
            log.info("🏷️ Multi-vehicle pricing for category: {}", category.getCategoryName().name());
            
            // Calculate price for each vehicle type in optimal allocation
            BigDecimal totalPrice = BigDecimal.ZERO;
            StringBuilder multiVehicleBreakdown = new StringBuilder();
            
            // Build vehicle allocation description
            multiVehicleBreakdown.append(String.format("⚠️ **Hàng %.1f tấn cần %s** để vận chuyển\n\n", 
                    weightInTons, optimalAllocation.getDescription()));
            
            // Build structured pricing display with tables
            multiVehicleBreakdown.append("📍 **Thông tin vận chuyển:**\n");
            multiVehicleBreakdown.append(String.format("- Khoảng cách: %.1f km\n", distance));
            multiVehicleBreakdown.append(String.format("- Trọng lượng hàng: %.1f tấn\n", weightInTons));
            multiVehicleBreakdown.append(String.format("- Phương án tối ưu: %s\n\n", optimalAllocation.getDescription()));
            
            // Detailed breakdown section first with clearer separator
            multiVehicleBreakdown.append("\n📋 **Chi tiết tính phí (cho 1 xe mỗi loại):**\n");
            for (Map.Entry<SizeRuleEntity, Integer> entry : optimalAllocation.getVehicleCounts().entrySet()) {
                SizeRuleEntity vehicle = entry.getKey();
                int count = entry.getValue();
                
                if (count > 0) {
                    PriceEstimateRequest request = new PriceEstimateRequest(distance, 
                            vehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000)), 
                            null, category.getCategoryName().name());
                    
                    PriceEstimateResult singleVehicleResult = estimatePriceForSpecificVehicle(request, vehicle);
                    
                    if (singleVehicleResult.isSuccess()) {
                        multiVehicleBreakdown.append(String.format("\n**%s:**\n", vehicle.getSizeRuleName()));
                        
                        // Extract detailed breakdown (remove first line with vehicle info)
                        String[] lines = singleVehicleResult.getBreakdown().split("\n");
                        for (int i = 1; i < lines.length; i++) { // Skip first line (vehicle info)
                            if (lines[i].trim().length() > 0) {
                                multiVehicleBreakdown.append(lines[i]).append("\n");
                            }
                        }
                    }
                }
            }
            
            // Summary section with "Tổng kết" header and clearer separator
            multiVehicleBreakdown.append("\n\n📊 **Tổng kết:**\n");
            
            for (Map.Entry<SizeRuleEntity, Integer> entry : optimalAllocation.getVehicleCounts().entrySet()) {
                SizeRuleEntity vehicle = entry.getKey();
                int count = entry.getValue();
                
                // Calculate price for this vehicle type
                PriceEstimateRequest request = new PriceEstimateRequest(distance, 
                        vehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000)), // Use max capacity per vehicle
                        null, category.getCategoryName().name());
                
                PriceEstimateResult singleVehicleResult = estimatePriceForSpecificVehicle(request, vehicle);
                
                if (singleVehicleResult.isSuccess()) {
                    BigDecimal vehicleTypePrice = BigDecimal.valueOf(singleVehicleResult.getEstimatedPrice()).multiply(BigDecimal.valueOf(count));
                    BigDecimal unitPrice = BigDecimal.valueOf(singleVehicleResult.getEstimatedPrice());
                    totalPrice = totalPrice.add(vehicleTypePrice);
                    
                    // Clean bullet format
                    multiVehicleBreakdown.append(String.format("• %s × %d = %,d VND\n", 
                            vehicle.getSizeRuleName(), count, vehicleTypePrice.intValue()));
                }
            }
            
            // Total summary with bold amount
            multiVehicleBreakdown.append(String.format("\n💰 **Tổng chi phí:** **%,d VND**\n", totalPrice.intValue()));
            
            // Cost comparison if we saved money
            SizeRuleEntity largestVehicle = allVehicles.stream()
                    .max(Comparator.comparing(SizeRuleEntity::getMaxWeight))
                    .orElse(null);
            if (largestVehicle != null) {
                int vehiclesNeeded = calculateNumberOfVehicles(weight, largestVehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000)));
                if (vehiclesNeeded > optimalAllocation.getTotalVehicleCount()) {
                    PriceEstimateRequest largestRequest = new PriceEstimateRequest(distance, 
                            largestVehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000)), 
                            null, category.getCategoryName().name());
                    PriceEstimateResult largestResult = estimatePriceForSpecificVehicle(largestRequest, largestVehicle);
                    
                    if (largestResult.isSuccess()) {
                        BigDecimal largestTotalPrice = BigDecimal.valueOf(largestResult.getEstimatedPrice()).multiply(BigDecimal.valueOf(vehiclesNeeded));
                        BigDecimal savings = largestTotalPrice.subtract(totalPrice);
                        if (savings.compareTo(BigDecimal.ZERO) > 0) {
                            multiVehicleBreakdown.append(String.format("✅ **Tiết kiệm:** %,d VND so với phương án %d xe %s\n\n", 
                                    savings.intValue(), vehiclesNeeded, largestVehicle.getSizeRuleName()));
                        }
                    }
                }
            }
            
            results.add(AllVehiclePriceResult.success(
                    totalPrice.doubleValue(),
                    optimalAllocation.getDescription(),
                    multiVehicleBreakdown.toString(),
                    false,
                    optimalAllocation.getTotalCapacity().doubleValue(),
                    category.getCategoryName().name()
            ));
        }
        
        return results;
    }

    /**
     * Tính giá cho một loại xe cụ thể - Sử dụng UnifiedPricingService để đảm bảo tính nhất quán
     */
    private PriceEstimateResult estimatePriceForSpecificVehicle(PriceEstimateRequest request, SizeRuleEntity selectedVehicle) {
        try {
            log.info("🧮 Calculating price for {}: weight={} kg, distance={} km", 
                    selectedVehicle.getSizeRuleName(), request.weight(), request.distance());

            // 1a. Kiểm tra nếu cần nhiều xe
            int numOfVehiclesNeeded = calculateNumberOfVehicles(request.weight(), selectedVehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000)));
            boolean needMultipleVehicles = numOfVehiclesNeeded > 1;

            // 2. Lấy category ID từ category name
            UUID categoryId = null;
            if (request.categoryName() != null && !request.categoryName().equals("Hàng thông thường")) {
                // Convert String to CategoryName enum for repository lookup
                CategoryName categoryNameEnum = CategoryName.fromString(request.categoryName());
                CategoryEntity category = categoryEntityService.findByCategoryName(categoryNameEnum).orElse(null);
                if (category != null) {
                    categoryId = category.getId();
                }
            }

            // 3. Sử dụng UnifiedPricingService để tính giá chính xác
            UnifiedPricingService.UnifiedPriceResult pricingResult = unifiedPricingService.calculatePrice(
                    selectedVehicle.getId(),
                    request.distance(),
                    numOfVehiclesNeeded, // Calculate for all vehicles at once
                    categoryId
            );

            if (!pricingResult.isSuccess()) {
                return PriceEstimateResult.error("Lỗi tính giá: " + pricingResult.getErrorMessage());
            }

            BigDecimal totalPrice = pricingResult.getTotalPrice();
            BigDecimal priceForOneVehicle = pricingResult.getAdjustedPriceForOneVehicle();

            // 4. Build breakdown cho specific vehicle
            StringBuilder breakdown = new StringBuilder();

            breakdown.append(String.format("🚛 **%s** (tải trọng: %.1f tấn)\n",
                    selectedVehicle.getSizeRuleName(),
                    selectedVehicle.getMaxWeight()));

            if (needMultipleVehicles) {
                breakdown.append(String.format("⚠️ Cần **%d xe** để vận chuyển %.1f tấn\n",
                        numOfVehiclesNeeded,
                        request.weight().divide(BigDecimal.valueOf(1000)).doubleValue()));
            }

            breakdown.append("📊 **Chi tiết tính phí:**\n");

            // Add distance tier breakdown for display
            addDistanceTierBreakdownSimple(breakdown, selectedVehicle, request.distance());

            // Add category adjustment for display
            if (categoryId != null) {
                addSimpleCategoryAdjustmentBreakdown(breakdown, request.categoryName());
            }

            if (needMultipleVehicles) {
                breakdown.append(String.format("💰 **Tổng: %,d × %d xe = %,d VND**\n", 
                        priceForOneVehicle.intValue(),
                        numOfVehiclesNeeded,
                        totalPrice.intValue()));
            } else {
                breakdown.append(String.format("💰 **Tổng: %,d VND**\n", totalPrice.intValue()));
            }

            return PriceEstimateResult.success(
                    totalPrice.doubleValue(),
                    selectedVehicle.getSizeRuleName(),
                    breakdown.toString()
            );

        } catch (Exception e) {
            log.error("❌ Error calculating price for vehicle: {}", selectedVehicle.getSizeRuleName(), e);
            return PriceEstimateResult.error("Không thể tính giá cho xe này");
        }
    }

    /**
     * Add simplified distance tier breakdown for specific vehicle display
     */
    private void addDistanceTierBreakdownSimple(StringBuilder breakdown, SizeRuleEntity selectedVehicle, BigDecimal distance) {
        List<DistanceRuleEntity> distanceRules = distanceRuleService.findAll()
                .stream()
                .sorted(Comparator.comparing(DistanceRuleEntity::getFromKm))
                .toList();

        BigDecimal remainingDistance = distance;

        for (DistanceRuleEntity distanceRule : distanceRules) {
            if (remainingDistance.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal from = distanceRule.getFromKm();
            BigDecimal to = distanceRule.getToKm();

            BasingPriceEntity basePrice = basingPriceService
                    .findBasingPriceEntityBysizeRuleEntityIdAndDistanceRuleEntityId(
                            selectedVehicle.getId(), distanceRule.getId())
                    .orElse(null);

            if (basePrice == null) {
                continue;
            }

            // Fixed tier (0-4km)
            if (from.compareTo(BigDecimal.ZERO) == 0 && to.compareTo(BigDecimal.valueOf(4)) == 0) {
                breakdown.append(String.format("- 0-4 km: %,d VND\n",
                        basePrice.getBasePrice().intValue()));
                remainingDistance = remainingDistance.subtract(to);
            } else {
                // Variable tier
                BigDecimal tierDistance = (to == null) ? remainingDistance : remainingDistance.min(to.subtract(from));
                BigDecimal tierPrice = basePrice.getBasePrice().multiply(tierDistance);

                breakdown.append(String.format("- %s-%s km: %.1f km × %,d = %,d VND\n",
                        from.intValue(),
                        to == null ? from.intValue() + "+" : to.intValue(),
                        tierDistance.doubleValue(),
                        basePrice.getBasePrice().intValue(),
                        tierPrice.intValue()));

                remainingDistance = remainingDistance.subtract(tierDistance);
            }
        }
    }

    /**
     * Add simplified category adjustment breakdown
     */
    private void addSimpleCategoryAdjustmentBreakdown(StringBuilder breakdown, String categoryName) {
        if (categoryName != null && !categoryName.equals("Hàng thông thường")) {
            // Convert String to CategoryName enum for repository lookup
            CategoryName categoryNameEnum = CategoryName.fromString(categoryName);
            CategoryEntity category = categoryEntityService.findByCategoryName(categoryNameEnum).orElse(null);
            if (category != null) {
                CategoryPricingDetailEntity pricingDetail = categoryPricingDetailService.findByCategoryId(category.getId());
                if (pricingDetail != null) {
                    BigDecimal multiplier = pricingDetail.getPriceMultiplier() != null 
                            ? pricingDetail.getPriceMultiplier() 
                            : BigDecimal.ONE;
                    BigDecimal extraFee = pricingDetail.getExtraFee() != null 
                            ? pricingDetail.getExtraFee() 
                            : BigDecimal.ZERO;
                    
                    breakdown.append(String.format("- %s: ×%.1f + %,d VND\n", 
                            categoryName,
                            multiplier.doubleValue(),
                            extraFee.intValue()));
                }
            }
        }
    }

    /**
     * Result class
     */
    public static class PriceEstimateResult {
        private final boolean success;
        private final Double estimatedPrice;
        private final String vehicleType;
        private final String breakdown;
        private final String errorMessage;

        private PriceEstimateResult(boolean success, Double estimatedPrice, String vehicleType,
                                    String breakdown, String errorMessage) {
            this.success = success;
            this.estimatedPrice = estimatedPrice;
            this.vehicleType = vehicleType;
            this.breakdown = breakdown;
            this.errorMessage = errorMessage;
        }

        public static PriceEstimateResult success(Double price, String vehicle, String breakdown) {
            return new PriceEstimateResult(true, price, vehicle, breakdown, null);
        }

        public static PriceEstimateResult error(String message) {
            return new PriceEstimateResult(false, null, null, null, message);
        }

        public boolean isSuccess() { return success; }
        public Double getEstimatedPrice() { return estimatedPrice; }
        public String getVehicleType() { return vehicleType; }
        public String getBreakdown() { return breakdown; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Result class for all vehicles pricing
     */
    public static class AllVehiclePriceResult {
        private final boolean success;
        private final Double estimatedPrice;
        private final String vehicleType;
        private final String breakdown;
        private final boolean recommended;
        private final Double maxLoad;
        private final String categoryName;
        private final String errorMessage;

        private AllVehiclePriceResult(boolean success, Double estimatedPrice, String vehicleType,
                                      String breakdown, boolean recommended, Double maxLoad, 
                                      String categoryName, String errorMessage) {
            this.success = success;
            this.estimatedPrice = estimatedPrice;
            this.vehicleType = vehicleType;
            this.breakdown = breakdown;
            this.recommended = recommended;
            this.maxLoad = maxLoad;
            this.categoryName = categoryName;
            this.errorMessage = errorMessage;
        }

        public static AllVehiclePriceResult success(Double price, String vehicle, String breakdown, 
                                                     boolean recommended, Double maxLoad, String categoryName) {
            return new AllVehiclePriceResult(true, price, vehicle, breakdown, recommended, maxLoad, categoryName, null);
        }

        public static AllVehiclePriceResult error(String message) {
            return new AllVehiclePriceResult(false, null, null, null, false, null, null, message);
        }

        public boolean isSuccess() { return success; }
        public Double getEstimatedPrice() { return estimatedPrice; }
        public String getVehicleType() { return vehicleType; }
        public String getBreakdown() { return breakdown; }
        public boolean isRecommended() { return recommended; }
        public Double getMaxLoad() { return maxLoad; }
        public String getCategoryName() { return categoryName; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Vehicle allocation result for optimal packing
     */
    private static class VehicleAllocation {
        private final Map<SizeRuleEntity, Integer> vehicleCounts;
        private final BigDecimal totalCapacity;
        private final String description;
        
        public VehicleAllocation(Map<SizeRuleEntity, Integer> vehicleCounts, String description) {
            this.vehicleCounts = vehicleCounts;
            this.totalCapacity = vehicleCounts.entrySet().stream()
                .map(entry -> entry.getKey().getMaxWeight().multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.description = description;
        }
        
        public Map<SizeRuleEntity, Integer> getVehicleCounts() { return vehicleCounts; }
        public BigDecimal getTotalCapacity() { return totalCapacity; }
        public String getDescription() { return description; }
        
        public int getTotalVehicleCount() {
            return vehicleCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
    
    private VehicleAllocation findOptimalVehicleAllocation(BigDecimal totalWeight, BigDecimal distance, List<SizeRuleEntity> allVehicles) {
        List<SizeRuleEntity> sortedVehicles = allVehicles.stream()
                .sorted(Comparator.comparing(SizeRuleEntity::getMaxWeight))
                .toList();

        Map<SizeRuleEntity, BigDecimal> vehicleCosts = calculateAllVehicleCosts(distance, sortedVehicles);

        Map<BigDecimal, VehicleAllocation> dp = new HashMap<>();
        dp.put(BigDecimal.ZERO, new VehicleAllocation(new HashMap<>(), ""));

        boolean improved = true;
        int iteration = 0;

        while (improved && iteration < 10) {
            improved = false;
            iteration++;

            List<BigDecimal> currentWeights = new ArrayList<>(dp.keySet());

            for (BigDecimal currentWeight : currentWeights) {
                VehicleAllocation currentAllocation = dp.get(currentWeight);

                for (SizeRuleEntity vehicle : sortedVehicles) {
                    BigDecimal vehicleCapacity = vehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000));
                    BigDecimal newWeight = currentWeight.add(vehicleCapacity);

                    if (newWeight.compareTo(totalWeight.add(sortedVehicles.get(sortedVehicles.size() - 1)
                            .getMaxWeight().multiply(BigDecimal.valueOf(1000)))) > 0) {
                        continue;
                    }

                    Map<SizeRuleEntity, Integer> newVehicleCounts = new HashMap<>(currentAllocation.getVehicleCounts());
                    newVehicleCounts.put(vehicle, newVehicleCounts.getOrDefault(vehicle, 0) + 1);

                    String newDesc = buildDescription(newVehicleCounts);
                    VehicleAllocation newAllocation = new VehicleAllocation(newVehicleCounts, newDesc);

                    BigDecimal newCost = calculateTotalCostFromMap(newVehicleCounts, vehicleCosts);

                    VehicleAllocation existing = dp.get(newWeight);
                    if (existing == null) {
                        dp.put(newWeight, newAllocation);
                        improved = true;
                    } else {
                        BigDecimal existingCost = calculateTotalCostFromMap(existing.getVehicleCounts(), vehicleCosts);
                        if (newCost.compareTo(existingCost) < 0) {
                            dp.put(newWeight, newAllocation);
                            improved = true;
                        }
                    }
                }
            }
        }

        VehicleAllocation bestAllocation = null;
        BigDecimal minCost = BigDecimal.valueOf(Double.MAX_VALUE);

        for (Map.Entry<BigDecimal, VehicleAllocation> entry : dp.entrySet()) {
            if (entry.getKey().compareTo(totalWeight) >= 0) {
                BigDecimal cost = calculateTotalCostFromMap(entry.getValue().getVehicleCounts(), vehicleCosts);
                if (cost.compareTo(minCost) < 0) {
                    minCost = cost;
                    bestAllocation = entry.getValue();
                }
            }
        }

        if (bestAllocation == null) {
            return new VehicleAllocation(Map.of(sortedVehicles.get(sortedVehicles.size() - 1),
                    calculateNumberOfVehicles(totalWeight, sortedVehicles.get(sortedVehicles.size() - 1)
                            .getMaxWeight().multiply(BigDecimal.valueOf(1000)))),
                    buildDescription(Map.of(sortedVehicles.get(sortedVehicles.size() - 1),
                            calculateNumberOfVehicles(totalWeight, sortedVehicles.get(sortedVehicles.size() - 1)
                                    .getMaxWeight().multiply(BigDecimal.valueOf(1000))))));
        }

        return bestAllocation;
    }

    /**
     * Find optimal vehicle allocation using efficient cost optimization
     * Uses dynamic programming approach with pruning for performance
     * Enhanced with caching and telemetry for optimization
        }
    }
    
    // Build up solutions using proper unbounded knapsack DP
    boolean improved = true;
    int iteration = 0;
    
    while (improved && iteration < 10) { // Limit iterations to prevent infinite loops
        improved = false;
        iteration++;
        log.info("🔄 DP iteration {} - exploring {} existing entries", iteration, dp.size());
        
        // Get all current weights to explore (copy to avoid concurrent modification)
        List<BigDecimal> currentWeights = new ArrayList<>(dp.keySet());
        
        for (BigDecimal currentWeight : currentWeights) {
            VehicleAllocation currentAllocation = dp.get(currentWeight);
            
            // Try adding each vehicle type
            for (SizeRuleEntity vehicle : sortedVehicles) {
                BigDecimal vehicleCapacity = vehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000));
                BigDecimal newWeight = currentWeight.add(vehicleCapacity);
                
                // Don't exceed total weight by more than the largest vehicle capacity
                if (newWeight.compareTo(totalWeight.add(sortedVehicles.get(sortedVehicles.size() - 1).getMaxWeight().multiply(BigDecimal.valueOf(1000)))) > 0) {
                    continue;
                }
                
                // Create new allocation
                Map<SizeRuleEntity, Integer> newVehicleCounts = new HashMap<>(currentAllocation.getVehicleCounts());
                newVehicleCounts.put(vehicle, newVehicleCounts.getOrDefault(vehicle, 0) + 1);
                
                String newDesc = buildDescription(newVehicleCounts);
                VehicleAllocation newAllocation = new VehicleAllocation(newVehicleCounts, newDesc);
                
                BigDecimal newCost = calculateTotalCostFromMap(newVehicleCounts, vehicleCosts);
                
                // Update DP if this is better for the weight
                VehicleAllocation existing = dp.get(newWeight);
                if (existing == null) {
                    dp.put(newWeight, newAllocation);
                    improved = true;
                    log.info("🆕 DP added: {} kg = {} (cost: {},000 VND)", newWeight, newDesc, newCost.intValue());
                } else {
                    BigDecimal existingCost = calculateTotalCostFromMap(existing.getVehicleCounts(), vehicleCosts);
                    if (newCost.compareTo(existingCost) < 0) {
        log.info("📊 Vehicle cost cache stats: {} hits, {} misses, hit rate: {:.1f}%", 
                cacheHits, cacheMisses, 
                (cacheHits + cacheMisses) > 0 ? (cacheHits * 100.0 / (cacheHits + cacheMisses)) : 0);
        
        // Build up solutions using proper unbounded knapsack DP
        boolean improved = true;
        int iteration = 0;
        
        while (improved && iteration < 10) { // Limit iterations to prevent infinite loops
            improved = false;
            iteration++;
            log.info("🔄 DP iteration {} - exploring {} existing entries", iteration, dp.size());
            
            // Get all current weights to explore (copy to avoid concurrent modification)
            List<BigDecimal> currentWeights = new ArrayList<>(dp.keySet());
            
            for (BigDecimal currentWeight : currentWeights) {
                VehicleAllocation currentAllocation = dp.get(currentWeight);
                
                // Try adding each vehicle type
                for (SizeRuleEntity vehicle : sortedVehicles) {
                    BigDecimal vehicleCapacity = vehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000));
                    BigDecimal newWeight = currentWeight.add(vehicleCapacity);
                    
                    // Don't exceed total weight by more than the largest vehicle capacity
                    if (newWeight.compareTo(totalWeight.add(sortedVehicles.get(sortedVehicles.size() - 1).getMaxWeight().multiply(BigDecimal.valueOf(1000)))) > 0) {
                        continue;
                    }
                    
                    // Create new allocation
                    Map<SizeRuleEntity, Integer> newVehicleCounts = new HashMap<>(currentAllocation.getVehicleCounts());
                    newVehicleCounts.put(vehicle, newVehicleCounts.getOrDefault(vehicle, 0) + 1);
                    
                    String newDesc = buildDescription(newVehicleCounts);
                    VehicleAllocation newAllocation = new VehicleAllocation(newVehicleCounts, newDesc);
                    
                    BigDecimal newCost = calculateTotalCostFromMap(newVehicleCounts, vehicleCosts);
                    
                    // Update DP if this is better for the weight
                    VehicleAllocation existing = dp.get(newWeight);
                    if (existing == null) {
                        dp.put(newWeight, newAllocation);
                        improved = true;
                        log.info("🆕 DP added: {} kg = {} (cost: {},000 VND)", newWeight, newDesc, newCost.intValue());
                    } else {
                        BigDecimal existingCost = calculateTotalCostFromMap(existing.getVehicleCounts(), vehicleCosts);
                        if (newCost.compareTo(existingCost) < 0) {
                            dp.put(newWeight, newAllocation);
                            improved = true;
                            log.info("✅ DP improved: {} kg = {} (cost: {},000 VND vs {},000 VND)", 
                                    newWeight, newDesc, newCost.intValue(), existingCost.intValue());
                        }
                    }
                }
            }
        }
        
        log.info("📊 DP table size: {} entries", dp.size());
        for (Map.Entry<BigDecimal, VehicleAllocation> entry : dp.entrySet()) {
            BigDecimal cost = calculateTotalCostFromMap(entry.getValue().getVehicleCounts(), vehicleCosts);
            log.info("   {} kg: {} = {},000 VND", entry.getKey(), entry.getValue().getDescription(), cost.intValue());
        }
        
        // Find best solution that can carry at least totalWeight
        VehicleAllocation bestAllocation = null;
        BigDecimal minCost = BigDecimal.valueOf(Double.MAX_VALUE);
        
        for (Map.Entry<BigDecimal, VehicleAllocation> entry : dp.entrySet()) {
            if (entry.getKey().compareTo(totalWeight) >= 0) {
                BigDecimal cost = calculateTotalCostFromMap(entry.getValue().getVehicleCounts(), vehicleCosts);
                if (cost.compareTo(minCost) < 0) {
                    minCost = cost;
                    bestAllocation = entry.getValue();
                    log.info("✅ Best found: {} = {},000 VND (capacity: {} tons)", 
                            bestAllocation.getDescription(), cost.intValue(), 
                            entry.getKey().divide(BigDecimal.valueOf(1000)));
                }
            }
        }
        
        if (bestAllocation == null) {
            // Fallback: use greedy approach
            log.warn("⚠️ DP failed, using greedy fallback for {} tons", totalWeight.divide(BigDecimal.valueOf(1000)));
            bestAllocation = findGreedyAllocation(totalWeight, sortedVehicles);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("✅ DP optimization completed in {}ms: {} tons → {}",
                totalTime, totalWeight.divide(BigDecimal.valueOf(1000)), bestAllocation.getDescription());
        
        return bestAllocation;
    }
    
    /**
     * Reconstruct VehicleAllocation from cached description string
     * Parses format like "1 Xe tải 5 tấn + 2 Xe tải 2 tấn" back to vehicle counts map
     */
    private VehicleAllocation reconstructAllocationFromDescription(String description, List<SizeRuleEntity> availableVehicles) {
        Map<SizeRuleEntity, Integer> vehicleCounts = new HashMap<>();
        
        // Parse description: "1 Xe tải 5 tấn + 2 Xe tải 2 tấn"
        String[] vehicleParts = description.split(" \\+ ");
        for (String part : vehicleParts) {
            part = part.trim();
            String[] tokens = part.split(" ", 3);
            if (tokens.length >= 2) {
                try {
                    int count = Integer.parseInt(tokens[0]);
                    String vehicleName = tokens[1] + (tokens.length > 2 ? " " + tokens[2] : "");
                    
                    // Find matching vehicle entity
                    SizeRuleEntity matchingVehicle = availableVehicles.stream()
                            .filter(v -> v.getSizeRuleName().equals(vehicleName))
                            .findFirst()
                            .orElse(null);
                    
                    if (matchingVehicle != null) {
                        vehicleCounts.put(matchingVehicle, count);
                    }
                } catch (NumberFormatException e) {
                    log.warn("⚠️ Failed to parse vehicle count from: {}", part);
                }
            }
        }
        
        return new VehicleAllocation(vehicleCounts, description);
    }
    
    /**
     * Greedy fallback for edge cases
     */
    private VehicleAllocation findGreedyAllocation(BigDecimal totalWeight, List<SizeRuleEntity> vehicles) {
        Map<SizeRuleEntity, Integer> allocation = new HashMap<>();
        BigDecimal remainingWeight = totalWeight;
        
        // Use largest vehicles first
        for (int i = vehicles.size() - 1; i >= 0; i--) {
            SizeRuleEntity vehicle = vehicles.get(i);
            BigDecimal vehicleCapacity = vehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000));
            
            BigDecimal needed = remainingWeight.divide(vehicleCapacity, 0, RoundingMode.UP);
            int count = needed.intValue();
            
            if (count > 0) {
                allocation.put(vehicle, count);
                remainingWeight = remainingWeight.subtract(vehicleCapacity.multiply(BigDecimal.valueOf(count)));
                break; // Only use one vehicle type in greedy fallback
            }
        }
        
        return new VehicleAllocation(allocation, buildDescription(allocation));
    }
    
    /**
     * Calculate total cost from pre-calculated vehicle costs
     */
    private BigDecimal calculateTotalCostFromMap(Map<SizeRuleEntity, Integer> allocation, Map<SizeRuleEntity, BigDecimal> vehicleCosts) {
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map.Entry<SizeRuleEntity, Integer> entry : allocation.entrySet()) {
            BigDecimal vehicleCost = vehicleCosts.get(entry.getKey());
            if (vehicleCost != null) {
                totalCost = totalCost.add(vehicleCost.multiply(BigDecimal.valueOf(entry.getValue())));
            }
        }
        return totalCost;
    }

    private Map<SizeRuleEntity, BigDecimal> calculateAllVehicleCosts(BigDecimal distance, List<SizeRuleEntity> allVehicles) {
        Map<SizeRuleEntity, BigDecimal> vehicleCosts = new HashMap<>();
        for (SizeRuleEntity vehicle : allVehicles) {
            BigDecimal cost = calculateSingleVehiclePrice(vehicle, distance);
            if (cost != null) {
                vehicleCosts.put(vehicle, cost);
            }
        }
        return vehicleCosts;
    }

    private BigDecimal calculateSingleVehiclePrice(SizeRuleEntity vehicle, BigDecimal distance) {
        try {
            PriceEstimateRequest request = new PriceEstimateRequest(
                    distance,
                    vehicle.getMaxWeight().multiply(BigDecimal.valueOf(1000)),
                    null,
                    null
            );
            PriceEstimateResult result = estimatePriceForSpecificVehicle(request, vehicle);
            if (!result.isSuccess() || result.getEstimatedPrice() == null) {
                return null;
            }
            return BigDecimal.valueOf(result.getEstimatedPrice());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Calculate total cost for a vehicle allocation (for final pricing with categories)
     */
    private BigDecimal calculateTotalCost(VehicleAllocation allocation, BigDecimal distance) {
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (Map.Entry<SizeRuleEntity, Integer> entry : allocation.getVehicleCounts().entrySet()) {
            SizeRuleEntity vehicle = entry.getKey();
            int count = entry.getValue();
            
            // Calculate price for this vehicle type (without category adjustments for comparison)
            UnifiedPricingService.UnifiedPriceResult pricingResult = unifiedPricingService.calculatePrice(
                    vehicle.getId(),
                    distance,
                    count,
                    null // No category for base comparison
            );
            
            if (pricingResult.isSuccess()) {
                totalCost = totalCost.add(pricingResult.getAdjustedPriceForOneVehicle().multiply(BigDecimal.valueOf(count)));
            }
        }
        
        return totalCost;
    }
    
    /**
     * Build description string from allocation map
     */
    private String buildDescription(Map<SizeRuleEntity, Integer> allocation) {
        StringBuilder desc = new StringBuilder();
        for (Map.Entry<SizeRuleEntity, Integer> entry : allocation.entrySet()) {
            if (desc.length() > 0) desc.append(" + ");
            desc.append(entry.getValue()).append(" ").append(entry.getKey().getSizeRuleName());
        }
        return desc.toString();
    }
    
    /**
     * Package information holder for dimension-based pricing
     */
    public static class PackageInfo {
        public final BigDecimal weight;      // Weight in tons
        public final BigDecimal length;      // Length in meters  
        public final BigDecimal width;       // Width in meters
        public final BigDecimal height;      // Height in meters
        
        public PackageInfo(BigDecimal weight, BigDecimal length, BigDecimal width, BigDecimal height) {
            this.weight = weight;
            this.length = length;
            this.width = width;
            this.height = height;
        }
    }
}
