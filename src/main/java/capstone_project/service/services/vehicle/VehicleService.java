package capstone_project.service.services.vehicle;

import capstone_project.dtos.request.vehicle.BatchUpdateLocationRequest;
import capstone_project.dtos.request.vehicle.UpdateLocationRequest;
import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleGetDetailsResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;

import java.util.List;
import java.util.UUID;

public interface VehicleService {
    List<VehicleResponse> getAllVehicles();
    VehicleGetDetailsResponse getVehicleById(UUID id);
    VehicleResponse createVehicle(VehicleRequest req);
    VehicleResponse updateVehicle(UUID id, UpdateVehicleRequest req);
    void updateVehicleLocation(UUID id, UpdateLocationRequest req);

    /**
     * Update vehicle location with rate limiting to prevent too frequent updates
     * @param id Vehicle ID
     * @param req Location update request
     * @param minIntervalSeconds Minimum seconds that must have elapsed since last update
     * @return true if location was updated, false if skipped due to rate limit or no change
     */
    boolean updateVehicleLocationWithRateLimit(UUID id, UpdateLocationRequest req, int minIntervalSeconds);

    /**
     * Update the locations of multiple vehicles in a single batch operation
     * @param batchRequest Batch of location updates
     * @return number of vehicles that were actually updated
     */
    int updateVehicleLocationsInBatch(BatchUpdateLocationRequest batchRequest);

    List<VehicleResponse> generateBulkVehicles(Integer count);
    
    /**
     * Get count of vehicles by status
     */
    long countVehiclesByStatus(String status);
    
    /**
     * Get count of vehicles by vehicle type
     */
    long countVehiclesByType(UUID vehicleTypeId);
}