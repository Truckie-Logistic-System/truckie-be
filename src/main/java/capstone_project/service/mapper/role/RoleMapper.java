package capstone_project.service.mapper.role;

import capstone_project.dtos.request.auth.RoleRequest;
import capstone_project.dtos.response.auth.RoleResponse;
import capstone_project.entity.auth.RoleEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse mapRoleResponse(final RoleEntity roleEntity);

    RoleEntity mapRoleRequestToEntity(final RoleRequest roleRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toRoleEntity(RoleRequest request, @MappingTarget RoleEntity entity);
}
