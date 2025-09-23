package capstone_project.dtos.request.vehicle;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced request class for creating and assigning vehicles to groups of order details
 * Structure matches the JSON format:
 * {
 *   "groupAssignments": [
 *     {
 *       "orderDetailIds": ["uuid1", "uuid2", ...],
 *       "vehicleId": "uuid",
 *       "driverId_1": "uuid",
 *       "driverId_2": "uuid",
 *       "description": ""
 *     }
 *   ]
 * }
 */
public record GroupedAssignmentRequest(
    List<OrderDetailGroupAssignment> groupAssignments
) {
    public record OrderDetailGroupAssignment(
        List<UUID> orderDetailIds,
        UUID vehicleId,
        UUID driverId_1,
        UUID driverId_2,
        String description
    ) {}
}
