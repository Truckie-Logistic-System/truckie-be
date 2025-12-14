package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.SimpleIssueResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced vehicle assignment response with full information for staff
 */
public record StaffVehicleAssignmentResponse(
    UUID id,
    VehicleResponse vehicle,
    StaffDriverResponse primaryDriver,
    StaffDriverResponse secondaryDriver,
    String status,
    String trackingCode,
    List<PenaltyHistoryResponse> penalties,
    VehicleFuelConsumptionResponse fuelConsumption,
    List<GetSealResponse> seals,
    List<JourneyHistoryResponse> journeyHistories,
    List<String> photoCompletions,
    List<SimpleIssueResponse> issues,
    List<VehicleAssignmentResponse.DeviceInfo> devices
) {}
