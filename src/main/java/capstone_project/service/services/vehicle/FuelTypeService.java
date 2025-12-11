package capstone_project.service.services.vehicle;

import capstone_project.dtos.request.vehicle.CreateFuelTypeRequest;
import capstone_project.dtos.request.vehicle.UpdateFuelTypeRequest;
import capstone_project.dtos.response.vehicle.FuelTypeResponse;

import java.util.List;
import java.util.UUID;

public interface FuelTypeService {
    
    /**
     * Get all fuel types (sorted by createdAt DESC)
     */
    List<FuelTypeResponse> getAllFuelTypes();
    
    /**
     * Get fuel type by ID
     */
    FuelTypeResponse getFuelTypeById(UUID id);
    
    /**
     * Search fuel types by name
     */
    List<FuelTypeResponse> searchFuelTypesByName(String name);
    
    /**
     * Create a new fuel type
     */
    FuelTypeResponse createFuelType(CreateFuelTypeRequest request);
    
    /**
     * Update an existing fuel type
     */
    FuelTypeResponse updateFuelType(UpdateFuelTypeRequest request);
    
    /**
     * Delete a fuel type
     */
    void deleteFuelType(UUID id);
}
