package capstone_project.service.services.impl;

import capstone_project.controller.dtos.request.RoleRequest;
import capstone_project.controller.dtos.response.RoleResponse;
import capstone_project.entity.RolesEntity;
import capstone_project.enums.ErrorEnum;
import capstone_project.exceptions.dto.BadRequestException;
import capstone_project.exceptions.dto.InternalServerException;
import capstone_project.exceptions.dto.NotFoundException;
import capstone_project.service.entityServices.RolesEntityService;
import capstone_project.service.mapper.RoleMapper;
import capstone_project.service.services.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RolesEntityService rolesEntityService;
    private final RoleMapper roleMapper;

    @Override
    public List<RoleResponse> getAllRoles() {
        log.info("Starting to get all roles");
        try {
            List<RolesEntity> roles = rolesEntityService.findAll();
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

        } catch (Exception e) {
            log.error("Error occurred while getting all roles", e);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        }
    }

    @Override
    public RoleResponse getRoleById(UUID id) {
        log.info("Starting to get role by ID: {}", id);
        try {
            Optional<RolesEntity> roleEntity = rolesEntityService.findById(id);

            return roleEntity.map(roleMapper::mapRoleResponse)
                    .orElseThrow(() -> {
                        log.warn("Role with ID {} not found", id);
                        return new NotFoundException(
                                ErrorEnum.NOT_FOUND.getMessage(),
                                ErrorEnum.NOT_FOUND.getErrorCode()
                        );
                    });

        } catch (Exception e) {
            log.error("Error occurred while getting role by ID: {}", id, e);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        }
    }

    @Override
    public RoleResponse getRoleByName(String roleName) {
        log.info("Starting to get role by name: {}", roleName);

        try {
            Optional<RolesEntity> roleEntity = rolesEntityService.findByRoleName(roleName);

            return roleEntity.map(roleMapper::mapRoleResponse)
                    .orElseThrow(() -> {
                        log.warn("Role with name {} not found", roleName);
                        return new NotFoundException(
                                ErrorEnum.NOT_FOUND.getMessage(),
                                ErrorEnum.NOT_FOUND.getErrorCode()
                        );
                    });

        } catch (Exception e) {
            log.error("Error occurred while getting role by name: {}", roleName, e);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        }
    }


    @Override
    public RoleResponse createRole(RoleRequest roleRequest) {
        log.info("Starting to create role with request: {}", roleRequest);
        try {
            Optional<RolesEntity> roleEntity = rolesEntityService.findByRoleName(roleRequest.getRoleName());
            if (roleEntity.isPresent()) {
                log.warn("Role with name {} is already existed", roleRequest.getRoleName());
                throw new BadRequestException(
                        ErrorEnum.ALREADY_EXISTED.getMessage(),
                        ErrorEnum.ALREADY_EXISTED.getErrorCode()
                );
            }

            RolesEntity rolesEntity = roleMapper.mapRoleRequestToEntity(roleRequest);
            rolesEntity.setCreatedAt(LocalDateTime.now());
            RolesEntity saved = rolesEntityService.save(rolesEntity);

            return roleMapper.mapRoleResponse(saved);

        } catch (Exception e) {
            log.error("Error occurred while creating role: {}", roleRequest, e);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        }
    }

    @Override
    public RoleResponse updateRole(UUID id, RoleRequest roleRequest) {
        log.info("Starting to update role with id: {}, request: {}", id, roleRequest);
        try {
            RolesEntity existingRole = rolesEntityService.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Role with ID {} not found", id);
                        return new NotFoundException(
                                ErrorEnum.NOT_FOUND.getMessage(),
                                ErrorEnum.NOT_FOUND.getErrorCode()
                        );
                    });

            roleMapper.toRoleEntity(roleRequest, existingRole);
            existingRole.setModifiedAt(LocalDateTime.now());
            RolesEntity updated = rolesEntityService.save(existingRole);

            return roleMapper.mapRoleResponse(updated);

        } catch (Exception e) {
            log.error("Error occurred while updating role with id {}: {}", id, e.getMessage(), e);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        }
    }

}
