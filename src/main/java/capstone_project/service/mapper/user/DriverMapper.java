package capstone_project.service.mapper.user;

import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.entity.user.driver.DriverEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    @Mapping(source = "user", target = "userResponse")
    DriverResponse mapDriverResponse(final DriverEntity driverEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toDriverEntity(UpdateDriverRequest request, @MappingTarget DriverEntity entity);
}
