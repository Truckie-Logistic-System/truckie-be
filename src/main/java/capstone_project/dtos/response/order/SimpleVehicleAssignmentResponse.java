package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.SimpleIssueResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;

import java.util.List;

public record SimpleVehicleAssignmentResponse(
    String id,
    VehicleResponse vehicle,
    SimpleDriverResponse primaryDriver,
    SimpleDriverResponse secondaryDriver,
    String status,
    String trackingCode,
    // Trip-related information
    List<SimpleIssueResponse> issues,
    List<String> photoCompletions,
    List<GetSealResponse> seals,
    List<JourneyHistoryResponse> journeyHistories
) {
    // Constructor for backward compatibility
    public SimpleVehicleAssignmentResponse(
            String id,
            VehicleResponse vehicle,
            SimpleDriverResponse primaryDriver,
            SimpleDriverResponse secondaryDriver,
            String status,
            String trackingCode) {
        this(id, vehicle, primaryDriver, secondaryDriver, status, trackingCode, null, null, null, null);
    }
}
