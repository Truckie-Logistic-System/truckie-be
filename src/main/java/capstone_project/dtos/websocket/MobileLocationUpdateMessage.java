package capstone_project.dtos.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobileLocationUpdateMessage {
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String licensePlateNumber; // Biển số xe để tiện hiển thị bên web
}
