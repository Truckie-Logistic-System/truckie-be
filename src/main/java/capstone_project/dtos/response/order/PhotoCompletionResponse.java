package capstone_project.dtos.response.order;

import capstone_project.dtos.response.device.DeviceResponse;

import java.util.UUID;

public record PhotoCompletionResponse(
        UUID id,
        String imageUrl,
        String description,
        UUID vehicleAssignmentId,
        DeviceResponse device
) {
}
