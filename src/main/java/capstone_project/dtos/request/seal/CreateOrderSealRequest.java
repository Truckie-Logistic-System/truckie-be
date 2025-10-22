package capstone_project.dtos.request.seal;

import java.util.UUID;

/**
 * Request to create order seal with seal code
 */
public record CreateOrderSealRequest(
        String sealCode,
        UUID vehicleAssignmentId,
        String description
) {}