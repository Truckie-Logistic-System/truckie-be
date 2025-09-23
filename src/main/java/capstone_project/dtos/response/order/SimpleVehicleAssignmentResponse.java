package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.SimpleIssueImageResponse;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;

import java.util.List;

public record SimpleVehicleAssignmentResponse(
    String id,
    String vehicleName,
    String licensePlateNumber,
    SimpleDriverResponse primaryDriver,
    SimpleDriverResponse secondaryDriver,
    String status,
    String trackingCode,
    // Trip-related information
    List<SimpleIssueImageResponse> issues,
    List<String> photoCompletions,
    List<GetOrderSealResponse> orderSeals,
    List<JourneyHistoryResponse> journeyHistories
) {
    // Constructor for backward compatibility
    public SimpleVehicleAssignmentResponse(
            String id,
            String vehicleName,
            String licensePlateNumber,
            SimpleDriverResponse primaryDriver,
            SimpleDriverResponse secondaryDriver,
            String status,
            String trackingCode) {
        this(id, vehicleName, licensePlateNumber, primaryDriver, secondaryDriver, status, trackingCode, null, null, null, null);
    }
}
