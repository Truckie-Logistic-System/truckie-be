package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.UUID;

public interface CategoryPricingDetailEntityService extends BaseEntityService<CategoryPricingDetailEntity, UUID> {
    CategoryPricingDetailEntity findByCategoryId(UUID categoryId);
}
