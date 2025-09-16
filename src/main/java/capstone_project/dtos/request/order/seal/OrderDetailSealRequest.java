package capstone_project.dtos.request.order.seal;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailSealRequest(
        @NotBlank(message ="Mô tả không được bỏ trống")
    String description,
    LocalDateTime sealDate,
    List<UUID> orderDetails
) {
}
