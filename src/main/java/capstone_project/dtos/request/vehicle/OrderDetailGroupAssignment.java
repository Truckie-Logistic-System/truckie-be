package capstone_project.dtos.request.vehicle;

import capstone_project.dtos.request.seal.SealInfo;

import java.util.List;
import java.util.UUID;

/**
 * Request class representing a group of order details to be assigned to a vehicle and drivers
 */
public record OrderDetailGroupAssignment(
    List<UUID> orderDetailIds,
    UUID vehicleId,
    UUID driverId_1,
    UUID driverId_2,
    String description,
    RouteInfo routeInfo,
    List<SealInfo> seals
) {}
