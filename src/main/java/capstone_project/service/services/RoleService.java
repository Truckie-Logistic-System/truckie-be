package capstone_project.service.services;

import capstone_project.controller.dtos.request.RoleRequest;
import capstone_project.controller.dtos.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(UUID id);

    RoleResponse getRoleByName(String roleName);

    RoleResponse createRole(RoleRequest roleRequest);

    RoleResponse updateRole(UUID id, RoleRequest roleRequest);
}
