package capstone_project.dtos.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileLocationUpdateMessage {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String licensePlateNumber; // Biển số xe để tiện hiển thị bên web
    private BigDecimal bearing; // Direction in degrees (0-360) from mobile
    private BigDecimal speed; // Speed in km/h from mobile
    private UUID vehicleAssignmentId; // Assignment for multi-trip off-route detection
}
