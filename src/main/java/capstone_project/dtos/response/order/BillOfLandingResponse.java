package capstone_project.dtos.response.order;

import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.vehicle.GetVehicleAssignmentForBillOfLandingResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record BillOfLandingResponse(
        String id,
        String code,

        UserResponse staff,
        CustomerResponse customer,
        GetOrderResponse order,
        List<GetVehicleAssignmentForBillOfLandingResponse> vehicleAssignmentResponse,

        String createdAt
) {
}
