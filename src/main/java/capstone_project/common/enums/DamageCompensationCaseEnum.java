package capstone_project.common.enums;

/**
 * Enum for DAMAGE compensation cases based on insurance policy.
 * 
 * Case 1: CÓ Bảo hiểm + CÓ Chứng từ → Bồi thường = Tỷ lệ hư hại × Giá trị khai báo (không giới hạn)
 * Case 2: CÓ Bảo hiểm + KHÔNG Chứng từ → BH vô hiệu, áp dụng giới hạn 10 × cước phí
 * Case 3: KHÔNG Bảo hiểm + CÓ Chứng từ → Giới hạn 10 × cước phí
 * Case 4: KHÔNG Bảo hiểm + KHÔNG Chứng từ → Giới hạn 10 × cước phí
 */
public enum DamageCompensationCaseEnum {
    /**
     * Case 1: Có bảo hiểm + Có chứng từ
     * Mức bồi thường = Tỷ lệ hư hại × Giá trị khai báo
     * Không áp dụng giới hạn pháp lý
     */
    CASE1_HAS_INS_HAS_DOC("Có bảo hiểm + Có chứng từ", "Bồi thường theo giá trị khai báo, không giới hạn"),
    
    /**
     * Case 2: Có bảo hiểm + Không có chứng từ
     * Bảo hiểm bị VÔ HIỆU HÓA
     * Mức bồi thường = MIN(Thiệt hại ước tính, 10 × Cước phí)
     */
    CASE2_HAS_INS_NO_DOC("Có bảo hiểm + Không chứng từ", "Bảo hiểm vô hiệu, áp dụng giới hạn 10× cước phí"),
    
    /**
     * Case 3: Không có bảo hiểm + Có chứng từ
     * Mức bồi thường = MIN(Thiệt hại theo chứng từ, 10 × Cước phí)
     */
    CASE3_NO_INS_HAS_DOC("Không bảo hiểm + Có chứng từ", "Áp dụng giới hạn 10× cước phí"),
    
    /**
     * Case 4: Không có bảo hiểm + Không có chứng từ
     * Mức bồi thường = MIN(Thiệt hại ước tính theo thị trường, 10 × Cước phí)
     */
    CASE4_NO_INS_NO_DOC("Không bảo hiểm + Không chứng từ", "Áp dụng giới hạn 10× cước phí, ước tính theo thị trường");
    
    private final String label;
    private final String description;
    
    DamageCompensationCaseEnum(String label, String description) {
        this.label = label;
        this.description = description;
    }
    
    public String getLabel() {
        return label;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determine compensation case based on insurance and document status
     */
    public static DamageCompensationCaseEnum determineCase(boolean hasInsurance, boolean hasDocuments) {
        if (hasInsurance && hasDocuments) {
            return CASE1_HAS_INS_HAS_DOC;
        } else if (hasInsurance && !hasDocuments) {
            return CASE2_HAS_INS_NO_DOC;
        } else if (!hasInsurance && hasDocuments) {
            return CASE3_NO_INS_HAS_DOC;
        } else {
            return CASE4_NO_INS_NO_DOC;
        }
    }
    
    /**
     * Check if this case applies legal limit (10 × freight fee)
     */
    public boolean appliesLegalLimit() {
        return this != CASE1_HAS_INS_HAS_DOC;
    }
}
