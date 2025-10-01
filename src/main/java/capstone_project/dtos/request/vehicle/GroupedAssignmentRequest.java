package capstone_project.dtos.request.vehicle;

import java.util.List;

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
 *       "description": "",
 *       "routeInfo": {
 *         "segments": [
 *           {
 *             "segmentOrder": 1,
 *             "startPointName": "Carrier",
 *             "endPointName": "Pickup",
 *             "startLatitude": 10.123456,
 *             "startLongitude": 106.123456,
 *             "endLatitude": 10.234567,
 *             "endLongitude": 106.234567,
 *             "distanceMeters": 15000,
 *             "pathCoordinates": [[106.123456, 10.123456], [106.234567, 10.234567]],
 *             "estimatedTollFee": 15000
 *           },
 *           {
 *             "segmentOrder": 2,
 *             "startPointName": "Pickup",
 *             "endPointName": "Delivery",
 *             "startLatitude": 10.234567,
 *             "startLongitude": 106.234567,
 *             "endLatitude": 10.345678,
 *             "endLongitude": 106.345678,
 *             "distanceMeters": 20000,
 *             "pathCoordinates": [[106.234567, 10.234567], [106.345678, 10.345678]],
 *             "estimatedTollFee": 25000
 *           }
 *         ],
 *         "totalTollFee": 40000
 *       }
 *     }
 *   ]
 * }
 */
public record GroupedAssignmentRequest(
    List<OrderDetailGroupAssignment> groupAssignments
) {}
