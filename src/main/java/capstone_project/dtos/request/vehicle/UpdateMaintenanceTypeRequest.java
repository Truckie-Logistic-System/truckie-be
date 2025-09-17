package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMaintenanceTypeRequest {
    @NotBlank
    @Size(max = 100)
    private String maintenanceTypeName;

    @Size(max = 200)
    private String description;

    private Boolean isActive;
}
