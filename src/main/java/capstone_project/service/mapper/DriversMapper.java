package capstone_project.service.mapper;

import capstone_project.controller.dtos.response.DriverResponse;
import capstone_project.entity.DriverEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DriversMapper {

    @Mapping(source = "user", target = "userResponse")
    DriverResponse mapDriverResponse(final DriverEntity driverEntity);
}
