package capstone_project.controller.admin;

import capstone_project.dtos.request.auth.RoleRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.auth.RoleResponse;
import capstone_project.service.services.auth.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${role.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class RolesController {

    private final RoleService roleService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable("id") UUID id) {
        final var result = roleService.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{roleName}/name")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleByName(@PathVariable("roleName") String roleName) {
        final var result = roleService.getRoleByName(roleName);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        final var result = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody RoleRequest roleRequest) {
        final var result = roleService.createRole(roleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getAllRoles(@PathVariable("id") UUID id,
                                                                @RequestBody RoleRequest roleRequest) {
        final var result = roleService.updateRole(id, roleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}