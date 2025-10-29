package capstone_project.repository.entityServices.pricing.impl;

import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.repository.repositories.pricing.BasingPriceRepository;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BasingPriceEntityServiceImpl implements BasingPriceEntityService {

    private final BasingPriceRepository basingPriceRepository;

    @Override
    public BasingPriceEntity save(BasingPriceEntity entity) {
        return basingPriceRepository.save(entity);
    }

    @Override
    public Optional<BasingPriceEntity> findEntityById(UUID uuid) {
        return basingPriceRepository.findById(uuid);
    }

    @Override
    public List<BasingPriceEntity> findAll() {
        return basingPriceRepository.findAll();
    }

    @Override
    public Optional<BasingPriceEntity> findBasingPriceEntityByVehicleTypeRuleEntityIdAndDistanceRuleEntityId(UUID vehicleRuleEntityId, UUID distanceRuleEntityId) {
        return basingPriceRepository.findBasingPriceEntityByVehicleTypeRuleEntityIdAndDistanceRuleEntityId(vehicleRuleEntityId, distanceRuleEntityId);
    }

    @Override
    public List<BasingPriceEntity> findAllByVehicleTypeRuleEntityId(UUID vehicleRuleEntityId) {
        return basingPriceRepository.findAllByVehicleTypeRuleEntityId(vehicleRuleEntityId);
    }
}
