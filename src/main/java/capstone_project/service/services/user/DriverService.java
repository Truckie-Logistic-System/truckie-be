package capstone_project.service.services.user;

import capstone_project.common.enums.VehicleTypeEnum;
import capstone_project.dtos.request.user.LicenseRenewalRequest;
import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.entity.user.driver.DriverEntity;

import java.util.List;
import java.util.UUID;

public interface DriverService {

    List<DriverResponse> getAllDrivers();

    List<DriverResponse> getAllDriversByUserRoleName(String roleName);

    DriverResponse getDriverById(UUID id);

    DriverResponse getDriverByUserId();

    DriverResponse updateDriver(UUID driverId, UpdateDriverRequest updateDriverRequest);

    DriverResponse updateDriverStatus(UUID driverId, String status);

    boolean isCheckClassDriverLicenseForVehicleType(DriverEntity driver, VehicleTypeEnum vehicleType);

    List<DriverResponse> generateBulkDrivers(Integer count);
    
    /**
     * Validate driver eligibility by phone number
     * Checks if driver exists, is active, and not currently assigned
     * 
     * @param phoneNumber Driver's phone number
     * @return Driver information if eligible
     */
    DriverResponse validateDriverByPhone(String phoneNumber);

    /**
     * Renew driver license
     * Updates license information and reactivates driver if inactive due to expired license
     * 
     * @param driverId Driver ID
     * @param request License renewal request
     * @return Updated driver information
     */
    DriverResponse renewDriverLicense(UUID driverId, LicenseRenewalRequest request);

    /**
     * Check if driver's license is expired
     * 
     * @param driver Driver entity
     * @return true if license is expired
     */
    boolean isLicenseExpired(DriverEntity driver);
}
