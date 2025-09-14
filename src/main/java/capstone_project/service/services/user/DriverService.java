package capstone_project.service.services.user;

import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.user.DriverResponse;

import java.util.List;
import java.util.UUID;

public interface DriverService {

    List<DriverResponse> getAllDrivers();

    List<DriverResponse> getAllDriversByUserRoleName(String roleName);

    DriverResponse getDriverById(UUID id);

    DriverResponse getDriverByUserId(UUID userId);

    DriverResponse updateDriver(UUID driverId, UpdateDriverRequest updateDriverRequest);

    DriverResponse updateDriverStatus(UUID driverId, String status);
}
