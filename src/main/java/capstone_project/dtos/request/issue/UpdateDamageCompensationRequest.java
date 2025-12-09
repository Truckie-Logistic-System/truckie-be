package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for updating DAMAGE issue compensation information.
 * Staff uses this to input assessment data after external appraisal.
 */
public record UpdateDamageCompensationRequest(
    @NotNull(message = "Issue ID is required")
    UUID issueId,
    
    @NotNull(message = "Damage assessment percent is required")
    @DecimalMin(value = "0", message = "Damage percent must be >= 0")
    @DecimalMax(value = "100", message = "Damage percent must be <= 100")
    BigDecimal damageAssessmentPercent,
    
    @NotNull(message = "Document status is required")
    Boolean damageHasDocuments,
    
    // Giá trị khai báo - bắt buộc khi có chứng từ
    BigDecimal damageDeclaredValue,
    
    // Giá trị ước tính theo thị trường - dùng khi không có chứng từ
    BigDecimal damageEstimatedMarketValue,
    
    // Số tiền bồi thường cuối cùng (nếu staff muốn điều chỉnh khác policy)
    BigDecimal damageFinalCompensation,
    
    // Lý do điều chỉnh (bắt buộc nếu final != policy)
    @Size(max = 500, message = "Adjust reason must not exceed 500 characters")
    String damageAdjustReason,
    
    // Ghi chú xử lý nội bộ
    @Size(max = 1000, message = "Handler note must not exceed 1000 characters")
    String damageHandlerNote,
    
    // Trạng thái xử lý: PROPOSED, APPROVED, REJECTED
    String damageCompensationStatus,
    
    // Fraud detection
    Boolean fraudDetected,
    
    @Size(max = 500, message = "Fraud reason must not exceed 500 characters")
    String fraudReason
) {
}
