package capstone_project.service.entityServices.auth.impl;

import capstone_project.entity.auth.RoleEntity;
import capstone_project.repository.auth.RoleRepository;
import capstone_project.service.entityServices.auth.RoleEntityService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RoleEntityServiceImpl implements RoleEntityService {

    private final RoleRepository roleRepository;

    @Override
    public Optional<RoleEntity> findByRoleName(String name) {
        return roleRepository.findByRoleName(name);
    }

    @Override
    public RoleEntity save(RoleEntity entity) {
        return roleRepository.save(entity);
    }

    @Override
    public Optional<RoleEntity> findContractRuleEntitiesById(UUID id) {
        return roleRepository.findById(id);
    }

    @Override
    public List<RoleEntity> findAll() {
        return roleRepository.findAll();
    }


}
