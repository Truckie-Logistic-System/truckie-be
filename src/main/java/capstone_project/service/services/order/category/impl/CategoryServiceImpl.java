package capstone_project.service.services.order.category.impl;

import capstone_project.common.enums.CategoryEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CategoryRequest;
import capstone_project.dtos.response.order.CategoryResponse;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import capstone_project.service.mapper.order.CategoryMapper;
import capstone_project.service.services.order.category.CategoryService;
import capstone_project.service.services.redis.RedisService;
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
    private final RedisService redisService;

    private static final String CATEGORY_ALL_CACHE_KEY = "categories:all";
    private static final String CATEGORY_ALL_BY_NAME_CACHE_KEY = "categories:all:name:";
    private static final String CATEGORY_BY_ID_CACHE_KEY_PREFIX = "category:";
    private static final String CATEGORY_BY_NAME_CACHE_KEY_PREFIX = "category:name:";

    @Override
    public List<CategoryResponse> getAllCategories() {

        List<CategoryEntity> cachedCategories = redisService.getList(
                CATEGORY_ALL_CACHE_KEY, CategoryEntity.class
        );

        List<CategoryEntity> categories;

        if (cachedCategories != null) {
            
            categories = cachedCategories;
        } else {
            
            categories = categoryEntityService.findAll();

            if (categories.isEmpty()) {
                log.warn("No categories found in the database");
                throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
            }

            redisService.save(CATEGORY_ALL_CACHE_KEY, categories);
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

        List<CategoryEntity> cachedCategories = redisService.getList(
                CATEGORY_ALL_BY_NAME_CACHE_KEY + categoryName, CategoryEntity.class
        );

        if (cachedCategories != null) {
            
            return cachedCategories.stream()
                    .map(categoryMapper::toCategoryResponse)
                    .toList();
        }

        List<CategoryEntity> categoryEntities = categoryEntityService.findByCategoryNameLikeIgnoreCase(categoryName);

        if (categoryEntities.isEmpty()) {
            log.warn("No categories found with categoryName containing: {}", categoryName);
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        redisService.save(CATEGORY_ALL_BY_NAME_CACHE_KEY + categoryName, categoryEntities);

        return categoryEntities.stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(UUID id) {

        String cacheKey = CATEGORY_BY_ID_CACHE_KEY_PREFIX + id;
        CategoryEntity cachedCategory = redisService.get(cacheKey, CategoryEntity.class);

        if (cachedCategory != null) {
            
            return categoryMapper.toCategoryResponse(cachedCategory);
        }

        CategoryEntity categoryEntity = categoryEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        redisService.save(cacheKey, categoryEntity);

        return categoryMapper.toCategoryResponse(categoryEntity);
    }

    @Override
    public CategoryResponse getCategoryByName(String categoryName) {

        String cacheKey = CATEGORY_BY_NAME_CACHE_KEY_PREFIX + categoryName;
        CategoryEntity cachedCategory = redisService.get(cacheKey, CategoryEntity.class);

        if (cachedCategory != null) {
            
            return categoryMapper.toCategoryResponse(cachedCategory);
        }

        CategoryEntity categoryEntity = categoryEntityService.findByCategoryName(categoryName)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        redisService.save(cacheKey, categoryEntity);

        return categoryMapper.toCategoryResponse(categoryEntity);
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {

        Optional<CategoryEntity> existingCategory = categoryEntityService.findByCategoryName(categoryRequest.categoryName());
        if (existingCategory.isPresent()) {
            log.warn("Category with name '{}' already exists", categoryRequest.categoryName());
            throw new NotFoundException("Category with this name already exists", ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        try {
            CategoryEnum.valueOf(categoryRequest.categoryName());
        } catch (NotFoundException e) {
            log.error("Invalid category type: {}", categoryRequest.categoryName());
            throw new NotFoundException("Invalid category type: " + categoryRequest.categoryName(), ErrorEnum.ENUM_INVALID.getErrorCode());
        }

        CategoryEntity categoryEntity = categoryMapper.mapRequestToEntity(categoryRequest);
        CategoryEntity savedCategory = categoryEntityService.save(categoryEntity);

        redisService.delete(CATEGORY_ALL_CACHE_KEY);

        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(UUID id, CategoryRequest categoryRequest) {

        CategoryEntity existingCategory = categoryEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

//        if (categoryRequest.categoryName() != null) {
//            try {
//                CategoryEnum.valueOf(categoryRequest.categoryName());
//            } catch (NotFoundException e) {
//                log.error("Invalid category type: {}", categoryRequest.categoryName());
//                throw new NotFoundException("Invalid category type: " + categoryRequest.categoryName(), ErrorEnum.ENUM_INVALID.getErrorCode());
//            }
//        }

        categoryMapper.toCategoryEntity(categoryRequest, existingCategory);
        CategoryEntity updatedCategory = categoryEntityService.save(existingCategory);

        redisService.delete(CATEGORY_ALL_CACHE_KEY);
        redisService.delete(CATEGORY_BY_ID_CACHE_KEY_PREFIX + id);

        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(UUID id) {

    }
}
