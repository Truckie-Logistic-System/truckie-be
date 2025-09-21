package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced vehicle assignment response with full information for staff
 */
public record StaffVehicleAssignmentResponse(
    UUID id,
    StaffVehicleResponse vehicle,
    StaffDriverResponse primaryDriver,
    StaffDriverResponse secondaryDriver,
    String status,
    List<PenaltyHistoryResponse> penalties,
    List<CameraTrackingResponse> cameraTrackings,
    VehicleFuelConsumptionResponse fuelConsumption,
    List<GetOrderSealResponse> orderSeals,
    List<JourneyHistoryResponse> journeyHistories,
    List<SimpleIssueImageResponse> issues
) {}
