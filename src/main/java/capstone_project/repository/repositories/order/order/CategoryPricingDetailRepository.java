package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.UUID;

public interface CategoryPricingDetailRepository extends BaseRepository<CategoryPricingDetailEntity> {

    CategoryPricingDetailEntity findByCategoryId(UUID categoryId);
}
