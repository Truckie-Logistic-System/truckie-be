package capstone_project.service.services.ai;

import capstone_project.common.enums.CategoryName;
import capstone_project.dtos.request.chat.ChatMessageRequest;
import capstone_project.dtos.request.chat.PriceEstimateRequest;
import capstone_project.dtos.response.chat.ChatMessageResponse;
import capstone_project.service.services.ai.GeminiService.ChatMessage;
import capstone_project.service.services.redis.RedisService;
import capstone_project.service.services.setting.CarrierSettingService;
import capstone_project.dtos.response.setting.CarrierSettingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final GeminiService geminiService;
    private final PriceCalculationService priceCalculationService;
    private final RedisService redisService;
    private final CarrierSettingService carrierSettingService;
    private final PricingDataService pricingDataService;
    private final CustomerDataService customerDataService;
    private final OrderTrackingService orderTrackingService;
    private final CustomerAnalyticsService customerAnalyticsService;

    private static final String SESSION_KEY_PREFIX = "chat:session:";
    private static final long SESSION_TTL_HOURS = 24;
    private static final String PERSONALITY_KEY_PREFIX = "chat:personality:";
    private static final long PERSONALITY_TTL_DAYS = 30;

    /**
     * Xử lý message từ user
     */
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Load hoặc tạo session
            String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
            String userId = request.userId();
            List<ChatMessage> history = loadChatHistory(sessionId, userId);

            // 2. Phát hiện intent (có phải tính phí không?)
            PriceIntent priceIntent = detectPriceIntent(request.message());

            // 3. Nếu là tính phí và có đủ thông tin → kiểm tra kích thước và tính giá phù hợp
            ChatMessageResponse.PriceEstimateData priceData = null;
            if (priceIntent.isPriceCalculation() && priceIntent.hasEnoughInfo()) {
                log.info("💰 Detected price calculation request - checking for dimensions");
                log.info("📊 Weight: {} kg, Distance: {} km, Category: {}", 
                        priceIntent.getWeight(), priceIntent.getDistance(), priceIntent.getCategoryName());
                
                // Extract package dimensions from message
                List<PriceCalculationService.PackageInfo> packageInfo = extractPackageDimensions(
                        request.message(), priceIntent.getWeight());
                
                List<PriceCalculationService.AllVehiclePriceResult> allResults;
                
                if (packageInfo != null) {
                    // ACCURATE pricing with dimensions using BinPacker
                    // Convert String categoryName to CategoryName enum for pricing methods
                    CategoryName categoryNameEnum = CategoryName.fromString(priceIntent.getCategoryName());
                    
                    log.info("📦 Using ACCURATE pricing with {} packages", packageInfo.size());
                    allResults = priceCalculationService.calculateAllVehiclesPriceWithDimensions(
                            priceIntent.getWeight(),
                            priceIntent.getDistance(),
                            categoryNameEnum.name(),
                            packageInfo
                    );
                } else {
                    // QUICK pricing (weight only)
                    log.info("⚡ Using QUICK pricing (weight only)");
                    // Convert String categoryName to CategoryName enum for pricing methods
                    CategoryName categoryNameEnum = CategoryName.fromString(priceIntent.getCategoryName());
                    allResults = priceCalculationService.calculateAllVehiclesPrice(
                            priceIntent.getWeight(),
                            priceIntent.getDistance(),
                            categoryNameEnum.name()
                    );
                }

                if (!allResults.isEmpty() && allResults.get(0).isSuccess()) {
                    // Format response by CATEGORY (not by vehicle)
                    StringBuilder categoryResponse = new StringBuilder();
                    categoryResponse.append(String.format("💰 **BÁO GIÁ VẬN CHUYỂN %.1f tấn, %s km**\n\n", 
                            priceIntent.getWeight().divide(BigDecimal.valueOf(1000)).doubleValue(),
                            priceIntent.getDistance().intValue()));

                    // Get vehicle info from first result (same vehicle for all categories)
                    String vehicleName = allResults.get(0).getVehicleType();
                    Double maxLoadTons = allResults.get(0).getMaxLoad();
                    
                    categoryResponse.append(String.format("🚛 **Xe phù hợp:** %s (tải trọng: %.1f tấn)\n\n", 
                            vehicleName, maxLoadTons));

                    // Show pricing for each category
                    for (PriceCalculationService.AllVehiclePriceResult result : allResults) {
                        if (result.isSuccess()) {
                            categoryResponse.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                            categoryResponse.append(String.format("📦 **%s**\n", result.getCategoryName()));
                            categoryResponse.append(result.getBreakdown());
                            categoryResponse.append("\n");
                        }
                    }

                    categoryResponse.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                    categoryResponse.append("💡 **Lưu ý:** Giá hàng dễ vỡ cao hơn do cần bảo quản đặc biệt.\n");
                    categoryResponse.append("⚠️ **GIÁ TRÊN CHỈ LÀ THAM KHẢO** - **CHƯA TÍNH KÍCH THƯỚC HÀNG HÓA**:\n");
                    categoryResponse.append("- Giá ước tính dựa trên **TRỌNG LƯỢNG** và **KHOẢNG CÁCH**\n");
                    categoryResponse.append("- Số lượng xe thực tế có thể khác do **KÍCH THƯỚC** kiện hàng\n");
                    categoryResponse.append("- Các yếu tố ảnh hưởng: điều kiện đường, thời gian, khu vực, phí cầu đường\n\n");
                    categoryResponse.append("🎯 **ĐỂ CÓ GIÁ CHÍNH XÁC HƠN:**\n");
                    categoryResponse.append("  • Cung cấp **SỐ LƯỢNG KIỆN HÀNG** và **KÍCH THƯỚC CHI TIẾT** (dài × rộng × cao, mét)\n");
                    categoryResponse.append("  • **ĐẶT HÀNG TRỰC TIẾP** qua hệ thống để nhận báo giá chính xác nhất\n");
                    categoryResponse.append("  • Nhân viên sẽ xác nhận giá cuối cùng sau khi kiểm tra thông tin đầy đủ\n\n");
                    categoryResponse.append("Bạn muốn đặt hàng loại hàng nào?");

                    // Save and return response
                    saveChatHistory(sessionId, userId, history, request.message(), categoryResponse.toString());

                    return ChatMessageResponse.builder()
                            .message(categoryResponse.toString())
                            .sessionId(sessionId)
                            .suggestedActions(buildSuggestedActions(true))
                            .build();
                }
            }

            // 4. Build system prompt với knowledge base (include customer data)
            String systemPrompt = buildSystemPrompt(request.userId());

            // 5. Thêm user message vào history
            history.add(new ChatMessage("user", request.message()));

            // 6. Gọi Gemini AI
            String aiResponse = geminiService.generateResponse(systemPrompt, history);

            // 7. Save history
            saveChatHistory(sessionId, userId, history, request.message(), aiResponse);

            // 8. Build response
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Chat processed in {}ms, session={}", duration, sessionId);

            return ChatMessageResponse.builder()
                    .message(aiResponse)
                    .sessionId(sessionId)
                    .priceEstimate(priceData)
                    .suggestedActions(buildSuggestedActions(false))
                    .build();

        } catch (Exception e) {
            log.error("❌ Error processing chat message", e);
            // Generate sessionId if not provided (lần đầu chat)
            String errorSessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
            return ChatMessageResponse.builder()
                    .message("Xin lỗi, tôi gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ hotline để được hỗ trợ.")
                    .sessionId(errorSessionId)
                    .suggestedActions(buildSuggestedActions(false))
                    .build();
        }
    }

    /**
     * Phát hiện intent tính phí từ message
     */
    private PriceIntent detectPriceIntent(String message) {
        String lowerMsg = message.toLowerCase();

        boolean isPriceCalculation = lowerMsg.contains("tính") && (lowerMsg.contains("phí") || lowerMsg.contains("giá"))
                || lowerMsg.contains("bao nhiêu tiền")
                || lowerMsg.contains("mất bao nhiêu")
                || lowerMsg.contains("chi phí")
                || (lowerMsg.contains("vận chuyển") && lowerMsg.contains("tấn"))
                || (lowerMsg.contains("vận chuyển") && lowerMsg.contains("kg"))
                || (lowerMsg.contains("chuyển") && lowerMsg.contains("tấn"))
                || (lowerMsg.contains("chuyển") && lowerMsg.contains("kg"));

        if (!isPriceCalculation) {
            return new PriceIntent(false, null, null, null);
        }

        // Extract weight (tấn, kg, tan, ki lô)
        BigDecimal weight = extractWeight(message);

        // Extract distance (km, cây số)
        BigDecimal distance = extractDistance(message);

        // Extract category
        String category = extractCategory(message);

        boolean hasEnoughInfo = weight != null && distance != null;

        return new PriceIntent(true, weight, distance, category, hasEnoughInfo);
    }

    private BigDecimal extractWeight(String message) {
        // Pattern: "5 tấn", "500 kg", "2.5 tan", "1000 ki lô"
        Pattern pattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(tấn|tan|kg|ki[\\s-]?l[oô]|kilogram)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String numberStr = matcher.group(1).replace(",", ".");
            String unit = matcher.group(2).toLowerCase();
            double value = Double.parseDouble(numberStr);

            // Convert to kg
            if (unit.contains("tấn") || unit.contains("tan")) {
                value *= 1000;
            }

            return BigDecimal.valueOf(value);
        }

        return null;
    }

    private BigDecimal extractDistance(String message) {
        // Pattern: "100 km", "50 cây số", "100km"
        Pattern pattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(km|c[aâ]y\\s*s[oố]|kilom[eé]t)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String numberStr = matcher.group(1).replace(",", ".");
            return BigDecimal.valueOf(Double.parseDouble(numberStr));
        }

        return null;
    }

    private String extractCategory(String message) {
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("dễ vỡ") || lowerMsg.contains("thủy tinh")) {
            return "Hàng dễ vỡ";
        } else if (lowerMsg.contains("nguy hiểm") || lowerMsg.contains("hóa chất")) {
            return "Hàng nguy hiểm";
        }
        return "Hàng thông thường";
    }

    /**
     * Extract package dimensions from user message
     * Returns list of PackageInfo if dimensions detected, null otherwise
     */
    private List<PriceCalculationService.PackageInfo> extractPackageDimensions(String message, BigDecimal totalWeight) {
        // Pattern for: "3 kiện 2x1.5x1 mét", "5 kiện hàng 3x2x1.5m", "2 kiện 2×1×1.5", etc.
        Pattern pattern = Pattern.compile("(\\d+)\\s*kiện.*?(\\d+\\.?\\d*)\\s*[xX×]\\s*(\\d+\\.?\\d*)\\s*[xX×]\\s*(\\d+\\.?\\d*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            try {
                int packageCount = Integer.parseInt(matcher.group(1));
                BigDecimal length = BigDecimal.valueOf(Double.parseDouble(matcher.group(2)));
                BigDecimal width = BigDecimal.valueOf(Double.parseDouble(matcher.group(3)));
                BigDecimal height = BigDecimal.valueOf(Double.parseDouble(matcher.group(4)));

                // Calculate weight per package
                BigDecimal weightPerPackage = totalWeight.divide(BigDecimal.valueOf(packageCount), 2, RoundingMode.HALF_UP);

                // Create package info list
                List<PriceCalculationService.PackageInfo> packages = new ArrayList<>();
                for (int i = 0; i < packageCount; i++) {
                    packages.add(new PriceCalculationService.PackageInfo(weightPerPackage, length, width, height));
                }

                log.info("📦 Extracted dimensions: {} packages, {}x{}x{}m, total weight {}kg", 
                        packageCount, length, width, height, totalWeight);

                return packages;

            } catch (Exception e) {
                log.warn("Failed to parse dimensions from message: {}", message, e);
                return null;
            }
        }

        return null;
    }

    /**
     * Build system prompt với knowledge base + customer data + personality
     */
    private String buildSystemPrompt(String userId) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Bạn là trợ lý AI của **Truckie** - hệ thống vận chuyển hàng hóa.\n\n");

        // Get and apply personality
        String personality = getPersonality(userId);
        prompt.append(getPersonalityInstructions(personality));
        prompt.append("\n");

        prompt.append("**VAI TRÒ:**\n");
        prompt.append("- Hỗ trợ khách hàng tìm hiểu về dịch vụ, quy trình, chính sách vận chuyển\n");
        prompt.append("- Trả lời câu hỏi về giá cả, thời gian, điều khoản\n");
        prompt.append("- Hướng dẫn khách hàng đặt hàng, theo dõi đơn hàng\n");
        prompt.append("- **Cung cấp thông tin cá nhân hóa** dựa trên dữ liệu khách hàng (nếu đã đăng nhập)\n");
        prompt.append("- **Theo dõi đơn hàng real-time** - cập nhật vị trí, ETA, tiến độ giao hàng\n");
        prompt.append("- **Phân tích và gợi ý thông minh** - dựa vào lịch sử đặt hàng, đưa ra gợi ý phù hợp\n\n");

        prompt.append("**QUY TẮC QUAN TRỌNG:**\n");
        prompt.append("1. Luôn trả lời bằng tiếng Việt\n");
        prompt.append("2. **QUAN TRỌNG NHẤT**: Dữ liệu được lấy TRỰC TIẾP từ database hệ thống (bảng giá, thông tin khách hàng, đơn hàng, GPS tracking, sự cố) - tuyệt đối chính xác và real-time\n");
        prompt.append("3. Dựa vào knowledge base bên dưới, KHÔNG bịa thông tin\n");
        prompt.append("4. Nếu user chưa đăng nhập (guest) và hỏi về thông tin cá nhân, ĐỀ NGHỊ user đăng nhập\n");
        prompt.append("5. Nếu không biết hoặc không chắc chắn, đề xuất liên hệ hotline\n");
        prompt.append("6. Khi nói về giá cả, LUÔN nhấn mạnh đây chỉ là giá tham khảo\n");
        prompt.append("7. Format markdown: dùng **bold**, bullet points, số thứ tự, headers ##\n");
        prompt.append("8. **Nhớ context conversation** - người dùng có thể hỏi tiếp về chủ đề trước (ví dụ: user hỏi 'Tính phí 3 tấn 50km', sau đó hỏi 'Còn 100km thì sao?' → bạn phải nhớ 3 tấn và chỉ thay đổi khoảng cách)\n");
        prompt.append("9. **TUYỆT ĐỐI KHÔNG** hiển thị thông tin kỹ thuật (database, API, system internals) cho khách hàng - chỉ dùng thông tin user-friendly\n");
        prompt.append("10. **TUYỆT ĐỐI QUAN TRỌNG - KHÔNG HIỂN THỊ DATA TIẾNG ANH**: Khi hiển thị data từ database, CHỈ ĐƯỢC hiển thị tiếng Việt, KHÔNG ĐƯỢC hiển thị giá trị gốc tiếng Anh trong ngoặc đơn hay bất kỳ đâu:\n");
        prompt.append("    - ❌ SAI: Chờ xử lý (PENDING), Xe tải 10 tấn (TRUCK_10_TON)\n");
        prompt.append("    - ✅ ĐÚNG: Chờ xử lý, Xe tải 10 tấn\n");
        prompt.append("    - ❌ SAI: Đang lấy hàng (PICKING_UP), Đã giao cho tài xế (ASSIGNED_TO_DRIVER)\n");
        prompt.append("    - ✅ ĐÚNG: Đang lấy hàng, Đã giao cho tài xế\n");
        prompt.append("    - ❌ SAI: Đã thanh toán (PAID), Đã hoàn thành (COMPLETED)\n");
        prompt.append("    - ✅ ĐÚNG: Đã thanh toán, Đã hoàn thành\n");
        prompt.append("    - **LƯU Ý**: NGƯỜI DÙNG MUỐN XEM DATA TIẾNG VIỆT TUYỆT ĐỐI, KHÔNG MUỐN THẤY TIẾNG ANH\n\n");
        
        prompt.append("11. **DỮ LIỆU THỐNG KÊ - TUYỆT ĐỐI GIỮ NGUYÊN CẤU TRÚC**: Khi nhận thống kê từ CustomerAnalyticsService:\n");
        prompt.append("    - **QUAN TRỌNG NHẤT**: Giữ nguyên CẤU TRÚC MARKDOWN CHÍNH XÁC TUYỆT ĐỐI\n");
        prompt.append("    - KHÔNG ĐƯỢC viết lại thành văn tự tự nhiên hay paragraph\n");
        prompt.append("    - Giữ nguyên tất cả bullet points (- **key**: value), headers (###), và định dạng\n");
        prompt.append("    - Backend đã gửi data có cấu trúc sẵn, chỉ cần hiển thị NGUYÊN BẢN\n");
        prompt.append("    - Đặc biệt giữ nguyên section '### 📊 Thống Kê Chính:' với tất cả metrics\n");
        prompt.append("    - Đặc biệt giữ nguyên section '### 📈 Chi Tiết Theo Trạng Thái:' với đầy đủ statuses\n");
        prompt.append("    - Đặc biệt giữ nguyên section '### 📈 So Sánh Với ...:' với chi tiết so sánh\n");
        prompt.append("    - ❌ SAI: Viết 'Tổng số kiện hàng: Bạn đã có 9 kiện hàng...'\n");
        prompt.append("    - ✅ ĐÚNG: Giữ nguyên '- **📦 Tổng số kiện hàng**: 9 kiện'\n\n");

                
        prompt.append("**QUY ĐỊNH HỆ THỐNG (PHẢI NHỚ RÕRANG):**\n");
        prompt.append("- **THANH TOÁN**: Hệ thống CHỈ hỗ trợ thanh toán online qua PayOS bằng QUÉT MÃ QR chuyển khoản. KHÔNG hỗ trợ: COD (tiền mặt), thẻ ATM nội địa, thẻ Visa/Mastercard, ví điện tử.\n");
        prompt.append("- **QUY TRÌNH THANH TOÁN**: Có 2 bước - (1) Đặt cọc sau khi ký hợp đồng, (2) Thanh toán TOÀN BỘ phần còn lại tối thiểu 1 ngày trước ngày lấy hàng. Khi giải thích quy trình đặt hàng, PHẢI nói đầy đủ cả 2 bước thanh toán này.\n");
        prompt.append("- **GIAO HÀNG KHẨN CẤP**: Hệ thống KHÔNG hỗ trợ giao hàng khẩn cấp/gấp. Ngày lấy hàng phải cách ngày đặt ít nhất 2 ngày.\n\n");
        
        prompt.append("**HƯỚNG DẪN SỬ DỤNG DỮ LIỆU TRONG KNOWLEDGE BASE:**\n");
        prompt.append("1. **📊 THỐNG KÊ ĐẶT HÀNG**:\n");
        prompt.append("   - Khi user hỏi 'Thống kê tháng này', 'Tôi dùng xe nào nhiều nhất?', 'Tuyến đường thường đi?'\n");
        prompt.append("   - Đọc section '📊 THỐNG KÊ ĐẶT HÀNG' trong Knowledge Base\n");
        prompt.append("   - Nếu có data, phân tích và trả lời cụ thể (số đơn, loại xe, tuyến đường, chi phí)\n");
        prompt.append("   - Nếu không có data, nói rõ: 'Bạn chưa có đơn hàng nào để thống kê'\n\n");
        prompt.append("2. **📍 ĐỊA CHỈ ĐÃ LƯU**:\n");
        prompt.append("   - Khi user hỏi về địa chỉ, đọc section '📍 Địa Chỉ Đã Lưu'\n");
        prompt.append("   - Nếu có địa chỉ trong data, liệt kê CỤ THỂ các địa chỉ đó. TUYỆT ĐỐI KHÔNG nói 'bạn chưa có địa chỉ'\n");
        prompt.append("   - Nếu thực sự không có địa chỉ (section nói 'Chưa có địa chỉ'), mới nói 'Bạn chưa lưu địa chỉ nào'\n\n");
        prompt.append("3. **🚛 LOẠI XE**:\n");
        prompt.append("   - Đọc section '🚛 Danh Sách Loại Xe' để trả lời về xe, kích thước, so sánh\n");
        prompt.append("   - Có đầy đủ thông tin 8 loại xe từ database\n\n");
        prompt.append("4. **GỢI Ý CÁ NHÂN HÓA**:\n");
        prompt.append("   - Khi user hỏi 'Gợi ý cho tôi', đọc 📊 THỐNG KÊ để phân tích:\n");
        prompt.append("     • Loại xe user dùng nhiều nhất → gợi ý tiếp tục dùng hoặc nâng cấp\n");
        prompt.append("     • Tuyến đường thường đi → gợi ý tối ưu thời gian, chi phí\n");
        prompt.append("     • Thời điểm đặt hàng → gợi ý đặt sớm để tránh cao điểm\n\n");

        prompt.append("**KNOWLEDGE BASE:**\n\n");
        prompt.append(loadKnowledgeBase(userId));

        return prompt.toString();
    }

    /**
     * Load knowledge base từ markdown files + real-time database + customer data
     */
    private String loadKnowledgeBase(String userId) {
        StringBuilder kb = new StringBuilder();

        // 1. CUSTOMER PERSONAL DATA (if logged in)
        if (userId != null && !userId.isEmpty()) {
            try {
                String customerInfo = customerDataService.generateCustomerInfo(userId);
                kb.append(customerInfo).append("\n\n---\n\n");
                log.info("✅ Loaded customer personal data for user: {}", userId);
            } catch (Exception e) {
                log.error("❌ Failed to load customer data", e);
            }

            // 1.1. REAL-TIME ORDER TRACKING
            try {
                String trackingInfo = orderTrackingService.generateTrackingInfo(userId);
                if (trackingInfo != null && !trackingInfo.isEmpty()) {
                    kb.append(trackingInfo).append("\n\n---\n\n");
                    log.info("✅ Loaded order tracking for user: {}", userId);
                }
            } catch (Exception e) {
                log.error("❌ Failed to load tracking data", e);
            }

            // 1.2. SPENDING ANALYTICS (default: current month)
            try {
                String analytics = customerAnalyticsService.generateSpendingAnalytics(userId, "month");
                if (analytics != null && !analytics.isEmpty()) {
                    kb.append(analytics).append("\n\n---\n\n");
                    log.info("✅ Loaded analytics for user: {}", userId);
                }
            } catch (Exception e) {
                log.error("❌ Failed to load analytics", e);
            }
        } else {
            kb.append("⚠️ **KHÁCH VÃNG LAI (Guest)**: User chưa đăng nhập. Nếu user hỏi về thông tin cá nhân, đơn hàng, địa chỉ → Đề nghị đăng nhập.\n\n---\n\n");
        }

        // 2. REAL-TIME PRICING DATA FROM DATABASE
        try {
            String realTimePricing = pricingDataService.generatePricingKnowledgeBase();
            kb.append(realTimePricing).append("\n\n---\n\n");
            log.info("✅ Loaded real-time pricing data from database");
        } catch (Exception e) {
            log.error("❌ Failed to load real-time pricing data", e);
            kb.append("⚠️ Không thể tải dữ liệu giá real-time. Sử dụng dữ liệu backup.\n\n");
        }

        // 3. Load carrier settings for contact info replacement
        CarrierSettingResponse carrierSettings = null;
        try {
            List<CarrierSettingResponse> allSettings = carrierSettingService.findAll();
            log.info("🔍 DEBUG: Found {} carrier settings in database", allSettings.size());
            
            carrierSettings = allSettings.stream()
                    .findFirst()
                    .orElse(null);
            
            if (carrierSettings != null) {
                log.info("✅ DEBUG: Loaded carrier settings - Name: {}, Phone: {}, Email: {}", 
                        carrierSettings.getCarrierName(), 
                        carrierSettings.getCarrierPhone(), 
                        carrierSettings.getCarrierEmail());
            } else {
                log.warn("⚠️ DEBUG: No carrier settings found in database");
            }
        } catch (Exception e) {
            log.error("❌ DEBUG: Failed to load carrier settings", e);
        }

        // 4. Static markdown files (FAQ, Process, Insurance Policy, etc.)
        String[] kbFiles = {
                "knowledge_base/faq.md",
                "knowledge_base/process.md",
                "knowledge_base/insurance_policy.md",
                "knowledge_base/terms_and_conditions.md"
        };

        for (String filePath : kbFiles) {
            try {
                ClassPathResource resource = new ClassPathResource(filePath);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String content = reader.lines().collect(Collectors.joining("\n"));
                    
                    // Replace technical database references with actual contact info
                    content = replaceContactPlaceholders(content, carrierSettings);
                    
                    kb.append(content).append("\n\n---\n\n");
                }
            } catch (Exception e) {
                log.warn("⚠️ Could not load knowledge base file: {}", filePath);
            }
        }

        if (kb.length() == 0) {
            kb.append("Không có knowledge base. Đề xuất khách hàng liên hệ hotline.\n");
        }

        return kb.toString();
    }

    /**
     * Replace technical database references with actual contact information
     */
    private String replaceContactPlaceholders(String content, CarrierSettingResponse carrierSettings) {
        log.info("🔧 DEBUG: Starting contact placeholder replacement");
        
        if (carrierSettings == null) {
            log.warn("⚠️ DEBUG: Carrier settings is null, using fallback");
            // Fallback values if carrier settings not available
            content = content.replace("(lấy từ carrier_settings trong DB)", "hotline hỗ trợ");
            return content;
        }

        log.info("📞 DEBUG: Carrier settings loaded - Phone: {}, Email: {}", 
                carrierSettings.getCarrierPhone(), carrierSettings.getCarrierEmail());

        // Check if content contains the placeholder before replacement (case insensitive)
        String hotlinePlaceholder = "(Lấy từ carrier_settings trong DB)";
        boolean containsPlaceholder = content.contains(hotlinePlaceholder);
        log.info("🔍 DEBUG: Content contains placeholder '{}': {}", hotlinePlaceholder, containsPlaceholder);
        
        if (containsPlaceholder) {
            // Replace hotline placeholder
            String hotline = carrierSettings.getCarrierPhone() != null ? 
                    carrierSettings.getCarrierPhone() : "hotline hỗ trợ";
            log.info("📞 DEBUG: Replacing placeholder with hotline: {}", hotline);
            content = content.replace(hotlinePlaceholder, hotline);
        }

        // Also check for lowercase version as fallback
        String lowercasePlaceholder = "(lấy từ carrier_settings trong DB)";
        if (!containsPlaceholder && content.contains(lowercasePlaceholder)) {
            String hotline = carrierSettings.getCarrierPhone() != null ? 
                    carrierSettings.getCarrierPhone() : "hotline hỗ trợ";
            log.info("� DEBUG: Replacing lowercase placeholder with hotline: {}", hotline);
            content = content.replace(lowercasePlaceholder, hotline);
        }

        log.info("✅ DEBUG: Contact replacement completed");
        return content;
    }

    /**
     * Load chat history từ Redis với user ID để tránh trộn lẫn tài khoản
     */
    private List<ChatMessage> loadChatHistory(String sessionId, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty for chat history loading");
        }
        
        String key = SESSION_KEY_PREFIX + userId + ":" + sessionId;
        String historyJson = redisService.getString(key);

        if (historyJson == null || historyJson.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Parse JSON string to list of messages
            String[] messages = historyJson.split("\\|\\|");
            List<ChatMessage> history = new ArrayList<>();
            
            for (String msg : messages) {
                String[] parts = msg.split(":::");
                if (parts.length == 2) {
                    history.add(new ChatMessage(parts[0], parts[1]));
                }
            }
            
            return history;
        } catch (Exception e) {
            log.error("❌ Error parsing chat history", e);
            return new ArrayList<>();
        }
    }

    /**
     * Save chat history vào Redis với user ID để tránh trộn lẫn tài khoản
     */
    private void saveChatHistory(String sessionId, String userId, List<ChatMessage> history,
                                  String userMessage, String assistantMessage) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty for chat history storage");
        }
        
        String key = SESSION_KEY_PREFIX + userId + ":" + sessionId;

        // Add new messages (don't modify original history list)
        List<ChatMessage> updatedHistory = new ArrayList<>(history);
        updatedHistory.add(new ChatMessage("user", userMessage));
        updatedHistory.add(new ChatMessage("assistant", assistantMessage));

        // Keep only last 10 messages (5 rounds)
        if (updatedHistory.size() > 10) {
            updatedHistory = updatedHistory.subList(updatedHistory.size() - 10, updatedHistory.size());
        }

        // Serialize to simple string format: role1:::content1||role2:::content2
        String historyJson = updatedHistory.stream()
                .map(msg -> msg.getRole() + ":::" + msg.getContent().replace(":::", "").replace("||", ""))
                .collect(Collectors.joining("||"));

        redisService.saveString(key, historyJson, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * Build suggested actions
     */
    private List<ChatMessageResponse.SuggestedAction> buildSuggestedActions(boolean afterPriceCalculation) {
        List<ChatMessageResponse.SuggestedAction> actions = new ArrayList<>();

        if (afterPriceCalculation) {
            actions.add(ChatMessageResponse.SuggestedAction.builder()
                    .label("Quy trình đặt hàng")
                    .action("Làm sao để đặt hàng?")
                    .build());
            actions.add(ChatMessageResponse.SuggestedAction.builder()
                    .label("Thời gian vận chuyển")
                    .action("Thời gian vận chuyển bao lâu?")
                    .build());
        } else {
            actions.add(ChatMessageResponse.SuggestedAction.builder()
                    .label("Tính phí vận chuyển")
                    .action("Tính phí vận chuyển")
                    .build());
            actions.add(ChatMessageResponse.SuggestedAction.builder()
                    .label("Quy trình đặt hàng")
                    .action("Quy trình đặt hàng như thế nào?")
                    .build());
        }

        return actions;
    }

    /**
     * Set AI personality for user
     */
    public void setPersonality(String userId, String personality) {
        String key = PERSONALITY_KEY_PREFIX + userId;
        redisService.saveString(key, personality, PERSONALITY_TTL_DAYS, TimeUnit.DAYS);
        log.info("✅ Set personality for user {}: {}", userId, personality);
    }

    /**
     * Get AI personality for user (default: FRIENDLY)
     */
    public String getPersonality(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "FRIENDLY"; // Default for guests
        }
        String key = PERSONALITY_KEY_PREFIX + userId;
        String personality = redisService.getString(key);
        return personality != null ? personality : "FRIENDLY";
    }

    /**
     * Get personality instructions for system prompt
     */
    private String getPersonalityInstructions(String personality) {
        return switch (personality.toUpperCase()) {
            case "PROFESSIONAL" -> """
                    **TONE: Chuyên Nghiệp**
                    - Sử dụng ngôn ngữ trang trọng, lịch sự
                    - Xưng hô: "Quý khách", "Chúng tôi"
                    - Trả lời ngắn gọn, súc tích, đi thẳng vào vấn đề
                    - Ít emoji, chỉ dùng khi cần thiết
                    """;
            case "FRIENDLY" -> """
                    **TONE: Thân Thiện**
                    - Sử dụng ngôn ngữ gần gũi, dễ hiểu
                    - Xưng hô: "Bạn", "Mình"
                    - Nhiệt tình, hỗ trợ tận tình
                    - Dùng emoji phù hợp để tăng sự thân thiện 😊
                    """;
            case "EXPERT" -> """
                    **TONE: Chuyên Gia**
                    - Giải thích chi tiết, chuyên sâu
                    - Cung cấp nhiều thông tin kỹ thuật
                    - Đưa ra phân tích, so sánh
                    - Giải thích công thức, quy trình rõ ràng
                    """;
            case "QUICK" -> """
                    **TONE: Nhanh Gọn**
                    - Trả lời cực kỳ ngắn gọn
                    - Chỉ thông tin cốt lõi, không giải thích dài
                    - Dùng bullet points
                    - Ưu tiên hành động nhanh
                    """;
            default -> """
                    **TONE: Thân Thiện** (default)
                    - Sử dụng ngôn ngữ gần gũi, dễ hiểu
                    - Nhiệt tình, hỗ trợ tận tình
                    """;
        };
    }

    /**
     * PriceIntent class
     */
    private static class PriceIntent {
        private final boolean isPriceCalculation;
        private final BigDecimal weight;
        private final BigDecimal distance;
        private final String categoryName;
        private final boolean hasEnoughInfo;

        public PriceIntent(boolean isPriceCalculation, BigDecimal weight, BigDecimal distance, String categoryName) {
            this(isPriceCalculation, weight, distance, categoryName, false);
        }

        public PriceIntent(boolean isPriceCalculation, BigDecimal weight, BigDecimal distance,
                           String categoryName, boolean hasEnoughInfo) {
            this.isPriceCalculation = isPriceCalculation;
            this.weight = weight;
            this.distance = distance;
            this.categoryName = categoryName;
            this.hasEnoughInfo = hasEnoughInfo;
        }

        public boolean isPriceCalculation() { return isPriceCalculation; }
        public BigDecimal getWeight() { return weight; }
        public BigDecimal getDistance() { return distance; }
        public String getCategoryName() { return categoryName; }
        public boolean hasEnoughInfo() { return hasEnoughInfo; }
    }
}
