package capstone_project.service.entityServices.order.order.impl;

import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.repository.order.order.CategoryPricingDetailRepository;
import capstone_project.service.entityServices.order.order.CategoryPricingDetailEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryPricingDetailEntityServiceImpl implements CategoryPricingDetailEntityService {

    private final CategoryPricingDetailRepository categoryPricingDetailRepository;

    @Override
    public CategoryPricingDetailEntity save(CategoryPricingDetailEntity entity) {
        return categoryPricingDetailRepository.save(entity);
    }

    @Override
    public Optional<CategoryPricingDetailEntity> findContractRuleEntitiesById(UUID uuid) {
        return categoryPricingDetailRepository.findById(uuid);
    }

    @Override
    public List<CategoryPricingDetailEntity> findAll() {
        return categoryPricingDetailRepository.findAll();
    }

    @Override
    public CategoryPricingDetailEntity findByCategoryId(UUID categoryId) {
        return categoryPricingDetailRepository.findByCategoryId(categoryId);
    }
}
