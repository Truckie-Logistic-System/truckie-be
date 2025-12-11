package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFuelTypeRequest {
    
    @NotBlank(message = "Tên loại nhiên liệu không được để trống")
    private String name;
    
    private String description;
}
