package capstone_project.service.entityServices.impl;

import capstone_project.entity.RolesEntity;
import capstone_project.repository.RoleRepository;
import capstone_project.service.entityServices.RolesEntityService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class RolesEntityServiceImpl implements RolesEntityService {

    private final RoleRepository roleRepository;

    @Override
    public Optional<RolesEntity> findByRoleName(String name) {
        return roleRepository.findByRoleName(name);
    }
}
