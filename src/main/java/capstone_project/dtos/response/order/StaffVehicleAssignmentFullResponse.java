package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.SimpleIssueResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import java.util.List;
import java.util.UUID;

/**
 * Full vehicle assignment response with all related data for staff detail page
 * Includes: vehicle, drivers, penalties, fuel consumption, seals, journey histories, 
 * photo completions, issues, order details, order info, and devices
 */
public record StaffVehicleAssignmentFullResponse(
    UUID id,
    VehicleResponse vehicle,
    StaffDriverResponse primaryDriver,
    StaffDriverResponse secondaryDriver,
    String status,
    String trackingCode,
    String description,
    List<PenaltyHistoryResponse> penalties,
    VehicleFuelConsumptionResponse fuelConsumption,
    List<GetSealResponse> seals,
    List<JourneyHistoryResponse> journeyHistories,
    List<String> photoCompletions,
    List<SimpleIssueResponse> issues,
    List<StaffOrderDetailResponse> orderDetails,
    SimpleOrderInfo order,
    List<DeviceInfo> devices
) {
    public record DeviceInfo(
        UUID id,
        String deviceCode,
        String manufacturer,
        String model,
        String ipAddress,
        String firmwareVersion,
        String deviceTypeName
    ) {}
}
