package capstone_project.dtos.response.device;

public record DeviceBulkCreateForVehiclesResponse(
        long totalVehicles,
        long vehiclesProcessed,
        long cameraDevicesCreated,
        long gpsDevicesCreated,
        long devicesSkipped
) {
}
