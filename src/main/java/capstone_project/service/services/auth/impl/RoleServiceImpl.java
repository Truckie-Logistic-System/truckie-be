package capstone_project.service.services.auth.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.auth.RoleRequest;
import capstone_project.dtos.response.auth.RoleResponse;
import capstone_project.entity.auth.RoleEntity;
import capstone_project.repository.entityServices.auth.RoleEntityService;
import capstone_project.service.mapper.role.RoleMapper;
import capstone_project.service.services.auth.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleEntityService roleEntityService;
    private final RoleMapper roleMapper;

    @Override
    public List<RoleResponse> getAllRoles() {
        
        List<RoleEntity> roles = roleEntityService.findAll();
        if (roles.isEmpty()) {
            log.warn("No roles found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return roles.stream()
                .map(roleMapper::mapRoleResponse)
                .collect(Collectors.toList());

    }

    @Override
    public RoleResponse getRoleById(UUID id) {
        
        Optional<RoleEntity> roleEntity = roleEntityService.findEntityById(id);

        return roleEntity.map(roleMapper::mapRoleResponse)
                .orElseThrow(() -> {
                    log.warn("Role with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });
    }

    @Override
    public RoleResponse getRoleByName(String roleName) {
        
        Optional<RoleEntity> roleEntity = roleEntityService.findByRoleName(roleName);

        return roleEntity.map(roleMapper::mapRoleResponse)
                .orElseThrow(() -> {
                    log.warn("Role with name {} not found", roleName);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });
    }

    @Override
    @Transactional
    public RoleResponse createRole(RoleRequest roleRequest) {
        
        Optional<RoleEntity> roleEntity = roleEntityService.findByRoleName(roleRequest.getRoleName());
        if (roleEntity.isPresent()) {
            log.warn("Role with name {} is already existed", roleRequest.getRoleName());
            throw new BadRequestException(
                    ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        RoleEntity rolesEntity = roleMapper.mapRoleRequestToEntity(roleRequest);
        RoleEntity saved = roleEntityService.save(rolesEntity);

        return roleMapper.mapRoleResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UUID id, RoleRequest roleRequest) {
        
        RoleEntity existingRole = roleEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Role with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        roleMapper.toRoleEntity(roleRequest, existingRole);
        RoleEntity updated = roleEntityService.save(existingRole);

        return roleMapper.mapRoleResponse(updated);
    }

}
