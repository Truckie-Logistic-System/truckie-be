package capstone_project.dtos.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for staff to cancel an order with a reason
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCancelOrderRequest {
    
    @NotBlank(message = "Lý do hủy không được để trống")
    @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự")
    private String cancellationReason;
}
