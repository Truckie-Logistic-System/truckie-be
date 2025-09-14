package capstone_project.dtos.response.vehicle;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VehicleGetDetailsResponse {
    private String id;
    private String licensePlateNumber;
    private String model;
    private String manufacturer;
    private Integer year;
    private BigDecimal capacity;
    private String status;
    private List<VehicleAssignmentResponse> vehicleAssignmentResponse;
    private List<VehicleMaintenanceResponse> vehicleMaintenanceResponse;
    private VehicleTypeResponse vehicleTypeResponse;

}