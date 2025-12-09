package capstone_project.dtos.response.user;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VehicleAssignmentSummary {
    private UUID id;
    private String trackingCode;
    private String description;
    private String status;
    private String vehiclePlateNumber;
    private String vehicleTypeDescription;
    
    private UUID driver1Id;
    private String driver1Name;
    private String driver1Phone;
    private String driver1LicenseNumber;
    
    private UUID driver2Id;
    private String driver2Name;
    private String driver2Phone;
    private String driver2LicenseNumber;
}
