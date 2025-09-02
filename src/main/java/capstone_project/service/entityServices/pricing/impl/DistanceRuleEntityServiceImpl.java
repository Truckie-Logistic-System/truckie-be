package capstone_project.service.entityServices.pricing.impl;

import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.repository.pricing.DistanceRuleRepository;
import capstone_project.service.entityServices.pricing.DistanceRuleEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistanceRuleEntityServiceImpl implements DistanceRuleEntityService {

    private final DistanceRuleRepository distanceRuleRepository;

    @Override
    public DistanceRuleEntity save(DistanceRuleEntity entity) {
        return distanceRuleRepository.save(entity);
    }

    @Override
    public Optional<DistanceRuleEntity> findEntityById(UUID uuid) {
        return distanceRuleRepository.findById(uuid);
    }

    @Override
    public List<DistanceRuleEntity> findAll() {
        return distanceRuleRepository.findAll();
    }
}
