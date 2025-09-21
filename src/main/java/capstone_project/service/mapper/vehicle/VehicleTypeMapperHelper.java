package capstone_project.service.mapper.vehicle;

import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import org.springframework.stereotype.Component;

@Component
public class VehicleTypeMapperHelper {

    private final VehicleTypeMapper vehicleTypeMapper;

    public VehicleTypeMapperHelper(VehicleTypeMapper vehicleTypeMapper) {
        this.vehicleTypeMapper = vehicleTypeMapper;
    }

    /**
     * Creates a VehicleTypeResponse with the specified vehicle count
     * @param vehicleType The vehicle type entity
     * @param vehicleCount The count of vehicles for this type
     * @return A VehicleTypeResponse with the vehicle count included
     */
    public VehicleTypeResponse toVehicleTypeResponseWithCount(VehicleTypeEntity vehicleType, long vehicleCount) {
        VehicleTypeResponse response = vehicleTypeMapper.toVehicleTypeResponse(vehicleType);
        return new VehicleTypeResponse(
                response.id(),
                response.vehicleTypeName(),
                response.description(),
                vehicleCount
        );
    }
}
