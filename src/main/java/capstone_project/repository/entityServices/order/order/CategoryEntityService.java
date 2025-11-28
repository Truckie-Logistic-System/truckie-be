package capstone_project.repository.entityServices.order.order;

import capstone_project.common.enums.CategoryName;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface CategoryEntityService extends BaseEntityService<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByCategoryName(CategoryName categoryName);
    
    void delete(CategoryEntity entity);
}
