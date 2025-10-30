package capstone_project.dtos.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLocationMessage {
    // Basic information
    private UUID vehicleId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String licensePlateNumber;

    // Additional vehicle information
    private String manufacturer;
    private String vehicleTypeName;

    // Assignment information
    private UUID vehicleAssignmentId;
    private String trackingCode;
    private String orderDetailStatus;

    // Driver information
    private String driver1Name;
    private String driver1Phone;
    private String driver2Name;
    private String driver2Phone;

    // Time information
    private LocalDateTime lastUpdated;
    
    // Smooth tracking information for frontend interpolation
    private BigDecimal bearing; // Direction in degrees (0-360)
    private BigDecimal speed; // Speed in km/h
    private BigDecimal velocityLat; // Latitude velocity (degrees/second)
    private BigDecimal velocityLng; // Longitude velocity (degrees/second)
}
