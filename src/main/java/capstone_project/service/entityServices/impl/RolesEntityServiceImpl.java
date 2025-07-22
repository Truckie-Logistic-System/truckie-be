package capstone_project.service.entityServices.impl;

import capstone_project.entity.RolesEntity;
import capstone_project.repository.RoleRepository;
import capstone_project.service.entityServices.RolesEntityService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RolesEntityServiceImpl implements RolesEntityService {

    private final RoleRepository roleRepository;

    @Override
    public Optional<RolesEntity> findByRoleName(String name) {
        return roleRepository.findByRoleName(name);
    }

    @Override
    public RolesEntity save(RolesEntity entity) {
        return roleRepository.save(entity);
    }

    @Override
    public Optional<RolesEntity> findById(UUID id) {
        return roleRepository.findById(id);
    }

    @Override
    public List<RolesEntity> findAll() {
        return roleRepository.findAll();
    }


}
