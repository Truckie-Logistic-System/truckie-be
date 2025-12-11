package capstone_project.service.mapper.user;

import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.user.DriverSummary;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.dtos.response.user.VehicleAssignmentSummary;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel="spring")
public interface PenaltyHistoryMapper {

    // Entity ➜ Response
    @Mapping(source="issueBy.id", target="driverId")
    @Mapping(source="issueBy", target="driverSummary", qualifiedByName = "driverToSummary")
    @Mapping(source="vehicleAssignmentEntity.id", target="vehicleAssignmentId")
    @Mapping(source="vehicleAssignmentEntity", target="vehicleAssignment")
    PenaltyHistoryResponse toPenaltyHistoryResponse(PenaltyHistoryEntity entity);

    // Request ➜ Entity (CREATE)
    @Mapping(target="id", ignore=true)
    @Mapping(target="createdAt", ignore=true)
    @Mapping(target="modifiedAt", ignore=true)
    @Mapping(target="issueBy.id",            source="driverId")
    @Mapping(target="vehicleAssignmentEntity.id", source="vehicleAssignmentId")
    PenaltyHistoryEntity toEntity(PenaltyHistoryRequest req);

    // UPDATE patch (MapStruct "merge")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target="issueBy.id",            source="driverId")
    @Mapping(target="vehicleAssignmentEntity.id", source="vehicleAssignmentId")
    void toEntity(PenaltyHistoryRequest req,
                  @MappingTarget PenaltyHistoryEntity entity);

    // VehicleAssignmentEntity ➜ VehicleAssignmentSummary
    @Mapping(source="vehicleEntity.licensePlateNumber", target="vehiclePlateNumber")
    @Mapping(source="vehicleEntity.vehicleTypeEntity.description", target="vehicleTypeDescription")
    @Mapping(source="vehicleEntity.manufacturer", target="vehicleBrand")
    @Mapping(source="vehicleEntity.model", target="vehicleModel")
    @Mapping(source="driver1", target="driver1Id", qualifiedByName = "driverToId")
    @Mapping(source="driver1", target="driver1Name", qualifiedByName = "driverToName")
    @Mapping(source="driver1", target="driver1Phone", qualifiedByName = "driverToPhone")
    @Mapping(source="driver1", target="driver1LicenseNumber", qualifiedByName = "driverToLicenseNumber")
    @Mapping(source="driver2", target="driver2Id", qualifiedByName = "driverToId")
    @Mapping(source="driver2", target="driver2Name", qualifiedByName = "driverToName")
    @Mapping(source="driver2", target="driver2Phone", qualifiedByName = "driverToPhone")
    @Mapping(source="driver2", target="driver2LicenseNumber", qualifiedByName = "driverToLicenseNumber")
    VehicleAssignmentSummary toVehicleAssignmentSummary(VehicleAssignmentEntity entity);

    @Named("driverToId")
    default UUID driverToId(DriverEntity driver) {
        return driver != null ? driver.getId() : null;
    }

    @Named("driverToName")
    default String driverToName(DriverEntity driver) {
        if (driver != null && driver.getUser() != null) {
            return driver.getUser().getFullName();
        }
        return null;
    }

    @Named("driverToPhone")
    default String driverToPhone(DriverEntity driver) {
        if (driver != null && driver.getUser() != null) {
            return driver.getUser().getPhoneNumber();
        }
        return null;
    }

    @Named("driverToLicenseNumber")
    default String driverToLicenseNumber(DriverEntity driver) {
        return driver != null ? driver.getDriverLicenseNumber() : null;
    }

    @Named("driverToSummary")
    default DriverSummary driverToSummary(DriverEntity driver) {
        if (driver == null) {
            return null;
        }

        UserEntity user = driver.getUser();

        return DriverSummary.builder()
                .driverId(driver.getId())
                .identityNumber(driver.getIdentityNumber())
                .driverLicenseNumber(driver.getDriverLicenseNumber())
                .cardSerialNumber(driver.getCardSerialNumber())
                .placeOfIssue(driver.getPlaceOfIssue())
                .dateOfIssue(driver.getDateOfIssue())
                .dateOfExpiry(driver.getDateOfExpiry())
                .licenseClass(driver.getLicenseClass())
                .dateOfPassing(driver.getDateOfPassing())
                .driverStatus(driver.getStatus())

                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : null)
                .fullName(user != null ? user.getFullName() : null)
                .email(user != null ? user.getEmail() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .gender(user != null ? user.getGender() : null)
                .dateOfBirth(user != null ? user.getDateOfBirth() : null)
                .imageUrl(user != null ? user.getImageUrl() : null)
                .userStatus(user != null ? user.getStatus() : null)
                .roleName(user != null && user.getRole() != null ? user.getRole().getRoleName() : null)
                .build();
    }
}