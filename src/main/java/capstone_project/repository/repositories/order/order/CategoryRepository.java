package capstone_project.repository.repositories.order.order;

import capstone_project.common.enums.CategoryName;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;

public interface CategoryRepository extends BaseRepository<CategoryEntity> {
    Optional<CategoryEntity> findByCategoryName(CategoryName categoryName);
}
