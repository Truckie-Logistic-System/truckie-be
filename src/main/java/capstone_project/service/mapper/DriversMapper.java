package capstone_project.service.mapper;

import capstone_project.controller.dtos.response.DriverResponse;
import capstone_project.entity.DriversEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DriversMapper {

    @Mapping(source = "user", target = "userResponse")
    DriverResponse mapDriverResponse(final DriversEntity driversEntity);
}
