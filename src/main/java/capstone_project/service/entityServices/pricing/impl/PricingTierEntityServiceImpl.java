package capstone_project.service.entityServices.pricing.impl;

import capstone_project.entity.pricing.PricingTierEntity;
import capstone_project.repository.pricing.PricingTierRepository;
import capstone_project.service.entityServices.pricing.PricingTierEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingTierEntityServiceImpl implements PricingTierEntityService {

    private final PricingTierRepository pricingTierRepository;

    @Override
    public PricingTierEntity save(PricingTierEntity entity) {
        return pricingTierRepository.save(entity);
    }

    @Override
    public Optional<PricingTierEntity> findById(UUID uuid) {
        return pricingTierRepository.findById(uuid);
    }

    @Override
    public List<PricingTierEntity> findAll() {
        return pricingTierRepository.findAll();
    }
}
