package capstone_project.service.entityServices.pricing.impl;

import capstone_project.entity.pricing.PricingRuleEntity;
import capstone_project.repository.pricing.PricingRuleRepository;
import capstone_project.service.entityServices.pricing.PricingRuleEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingRuleEntityServiceImpl implements PricingRuleEntityService {

    private final PricingRuleRepository pricingRuleRepository;

    @Override
    public PricingRuleEntity save(PricingRuleEntity entity) {
        return pricingRuleRepository.save(entity);
    }

    @Override
    public Optional<PricingRuleEntity> findById(UUID uuid) {
        return pricingRuleRepository.findById(uuid);
    }

    @Override
    public List<PricingRuleEntity> findAll() {
        return pricingRuleRepository.findAll();
    }
}
