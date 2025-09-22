package capstone_project.dtos.request.vehicle;

import java.util.Map;

/**
 * Request class for creating and assigning vehicles to order details
 * Structure matches the JSON format:
 * {
 *   "assignments": {
 *     "TRACKING_CODE_1": {
 *       "vehicleId": "uuid",
 *       "driverId_1": "uuid",
 *       "driverId_2": "uuid",
 *       "description": ""
 *     }
 *   }
 * }
 */
public record CreateAndAssignForDetailsRequest(
        Map<String, VehicleAssignmentRequest> assignments
) {
}
