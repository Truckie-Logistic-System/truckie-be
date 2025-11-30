package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.common.enums.CategoryName;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.repository.repositories.order.order.CategoryRepository;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CategoryEntityServiceImpl implements CategoryEntityService {

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryEntity save(CategoryEntity entity) {
        return categoryRepository.save(entity);
    }

    @Override
    public Optional<CategoryEntity> findEntityById(UUID uuid) {
        return categoryRepository.findById(uuid);
    }

    @Override
    public List<CategoryEntity> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<CategoryEntity> findByCategoryName(CategoryName categoryName) {
        return categoryRepository.findByCategoryName(categoryName);
    }
    
    @Override
    public void delete(CategoryEntity entity) {
        categoryRepository.delete(entity);
    }
}
