package capstone_project.service.mapper;

import capstone_project.controller.dtos.request.RoleRequest;
import capstone_project.controller.dtos.response.RoleResponse;
import capstone_project.entity.RolesEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse mapRoleResponse(final RolesEntity rolesEntity);

    RolesEntity mapRoleRequestToEntity(final RoleRequest roleRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toRoleEntity(RoleRequest request, @MappingTarget RolesEntity entity);
}
