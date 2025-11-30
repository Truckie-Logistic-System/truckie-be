package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for driver to confirm return delivery at pickup location
 */
public record ConfirmReturnDeliveryRequest(
        @NotNull
        UUID issueId,
        
        @NotNull
        List<String> returnDeliveryImages // URLs ảnh xác nhận trả hàng về pickup (có thể nhiều ảnh)
) {
}
