package capstone_project.dtos.request.offroute;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to confirm driver is safe after off-route event
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOffRouteSafeRequest {
    @NotNull(message = "Off-route event ID is required")
    private UUID offRouteEventId;
    
    private String notes;
}
