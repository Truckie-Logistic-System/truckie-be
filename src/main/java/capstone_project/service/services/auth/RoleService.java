package capstone_project.service.services.auth;

import capstone_project.dtos.request.auth.RoleRequest;
import capstone_project.dtos.response.auth.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(UUID id);

    RoleResponse getRoleByName(String roleName);

    RoleResponse createRole(RoleRequest roleRequest);

    RoleResponse updateRole(UUID id, RoleRequest roleRequest);
}
