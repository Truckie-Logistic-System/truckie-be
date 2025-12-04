package capstone_project.dtos.request.offroute;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to create an issue from an off-route event
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOffRouteIssueRequest {
    @NotNull(message = "Off-route event ID is required")
    private UUID offRouteEventId;
    
    private String description;
    private String staffNotes;
}
