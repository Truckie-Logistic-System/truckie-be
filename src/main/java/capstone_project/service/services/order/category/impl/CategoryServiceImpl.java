package capstone_project.service.services.order.category.impl;

import capstone_project.common.enums.CategoryName;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CategoryRequest;
import capstone_project.dtos.response.order.CategoryResponse;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import capstone_project.service.mapper.order.CategoryMapper;
import capstone_project.service.services.order.category.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryEntityService categoryEntityService;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<CategoryEntity> categories = categoryEntityService.findAll();

        if (categories.isEmpty()) {
            log.warn("No categories found in the database");
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        return categories.stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getAllCategoriesByCategoryName(String categoryName) {

        if (categoryName == null || categoryName.isBlank()) {
            log.warn("Category name is null or blank");
            throw new NotFoundException(ErrorEnum.INVALID.getMessage(), ErrorEnum.INVALID.getErrorCode());
        }

        // Convert String to CategoryName enum for exact matching
        CategoryName categoryNameEnum = CategoryName.fromString(categoryName);
        Optional<CategoryEntity> categoryEntity = categoryEntityService.findByCategoryName(categoryNameEnum);

        if (categoryEntity.isEmpty()) {
            log.warn("No category found with enum name: {}", categoryNameEnum);
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        return List.of(categoryMapper.toCategoryResponse(categoryEntity.get()));
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {
        CategoryEntity categoryEntity = categoryEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        return categoryMapper.toCategoryResponse(categoryEntity);
    }

    @Override
    public CategoryResponse getCategoryByName(String categoryName) {
        // Convert String to CategoryName enum for repository lookup
        CategoryName categoryNameEnum = CategoryName.fromString(categoryName);
        CategoryEntity categoryEntity = categoryEntityService.findByCategoryName(categoryNameEnum)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        return categoryMapper.toCategoryResponse(categoryEntity);
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {

        // Convert String to CategoryName enum for repository lookup
        CategoryName categoryNameEnum = CategoryName.fromString(categoryRequest.categoryName());
        Optional<CategoryEntity> existingCategory = categoryEntityService.findByCategoryName(categoryNameEnum);
        if (existingCategory.isPresent()) {
            log.warn("Category with name '{}' already exists", categoryRequest.categoryName());
            throw new NotFoundException("Category with this name already exists", ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        CategoryEntity categoryEntity = categoryMapper.mapRequestToEntity(categoryRequest);
        CategoryEntity savedCategory = categoryEntityService.save(categoryEntity);

        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(UUID id, CategoryRequest categoryRequest) {

        CategoryEntity existingCategory = categoryEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        // Check if new category name conflicts with existing category
        if (categoryRequest.categoryName() != null && !categoryRequest.categoryName().equals(existingCategory.getCategoryName().name())) {
            // Convert String categoryName to CategoryName enum for repository lookup
            CategoryName categoryNameEnum = CategoryName.fromString(categoryRequest.categoryName());
            Optional<CategoryEntity> conflictingCategory = categoryEntityService.findByCategoryName(categoryNameEnum);
            if (conflictingCategory.isPresent()) {
                log.warn("Category with name '{}' already exists", categoryRequest.categoryName());
                throw new NotFoundException("Category with this name already exists", ErrorEnum.ALREADY_EXISTED.getErrorCode());
            }
        }

        categoryMapper.toCategoryEntity(categoryRequest, existingCategory);
        CategoryEntity updatedCategory = categoryEntityService.save(existingCategory);

        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(UUID id) {
        CategoryEntity existingCategory = categoryEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        // Check if category is being used by any order details
        // Note: This check should be done at the repository level with a proper query
        // For now, we'll proceed with the deletion and let the database handle foreign key constraints
        
        try {
            categoryEntityService.delete(existingCategory);

            log.info("Successfully deleted category with id: {} and name: {}", id, existingCategory.getCategoryName().name());
        } catch (Exception e) {
            log.error("Failed to delete category with id: {}. It may be in use by other records.", id, e);
            throw new NotFoundException("Cannot delete category: it may be in use by order details", ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode());
        }
    }
}
