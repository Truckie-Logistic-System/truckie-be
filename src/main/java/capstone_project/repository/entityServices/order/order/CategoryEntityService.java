package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryEntityService extends BaseEntityService<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByCategoryName(String categoryName);

    List<CategoryEntity> findByCategoryNameLikeIgnoreCase(String categoryName);
}
