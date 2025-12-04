package capstone_project.dtos.response.issue;

import java.math.BigDecimal;

/**
 * Response DTO for DAMAGE compensation details.
 * Contains all calculated values and policy information for staff review.
 */
public record DamageCompensationResponse(
    // === Input data ===
    
    // Tỷ lệ hư hỏng (%) - do staff nhập
    BigDecimal damageAssessmentPercent,
    
    // Khách hàng có bảo hiểm không (từ Order)
    Boolean hasInsurance,
    
    // Khách hàng có cung cấp chứng từ không
    Boolean damageHasDocuments,
    
    // Giá trị khai báo theo chứng từ (VNĐ)
    BigDecimal damageDeclaredValue,
    
    // Giá trị ước tính theo thị trường (VNĐ)
    BigDecimal damageEstimatedMarketValue,
    
    // === Calculated values ===
    
    // Cước phí vận chuyển (VNĐ) - từ Contract
    BigDecimal damageFreightFee,
    
    // Giới hạn pháp lý = 10 × cước phí (VNĐ)
    BigDecimal damageLegalLimit,
    
    // Thiệt hại ước tính = giá trị × tỷ lệ hư hỏng (VNĐ)
    BigDecimal damageEstimatedLoss,
    
    // Mức bồi thường theo đúng policy (VNĐ)
    BigDecimal damagePolicyCompensation,
    
    // Mức bồi thường cuối cùng (VNĐ)
    BigDecimal damageFinalCompensation,
    
    // === Policy info ===
    
    // Kịch bản bồi thường
    String damageCompensationCase,
    
    // Label của kịch bản (tiếng Việt)
    String damageCompensationCaseLabel,
    
    // Mô tả kịch bản
    String damageCompensationCaseDescription,
    
    // Có áp dụng giới hạn pháp lý không
    Boolean appliesLegalLimit,
    
    // === Processing info ===
    
    // Lý do điều chỉnh
    String damageAdjustReason,
    
    // Ghi chú xử lý nội bộ
    String damageHandlerNote,
    
    // Trạng thái xử lý
    String damageCompensationStatus,
    
    // Label trạng thái (tiếng Việt)
    String damageCompensationStatusLabel
) {
}
