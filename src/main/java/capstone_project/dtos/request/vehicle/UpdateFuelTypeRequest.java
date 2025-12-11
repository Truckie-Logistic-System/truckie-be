package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFuelTypeRequest {
    
    @NotNull(message = "ID không được để trống")
    private UUID id;
    
    @NotBlank(message = "Tên loại nhiên liệu không được để trống")
    private String name;
    
    private String description;
}
