package capstone_project.dtos.request.vehicle;

import capstone_project.dtos.request.order.DetailsForAssignemntRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateAndAssignForDetailsRequest(
        Map<DetailsForAssignemntRequest, VehicleAssignmentRequest> assignments
) {
}
