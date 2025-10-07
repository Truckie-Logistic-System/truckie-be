package capstone_project.dtos.request.order.seal;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record OrderSealRequest(
    @NotNull(message = "Vehicle assignment ID không được bỏ trống")
    UUID vehicleAssignmentId,

    @NotNull(message = "Hình ảnh seal không được bỏ trống")
    MultipartFile sealImage
) {
}
