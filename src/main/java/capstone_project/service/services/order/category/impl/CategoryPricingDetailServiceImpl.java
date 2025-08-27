package capstone_project.service.services.order.category.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CategoryPricingDetailRequest;
import capstone_project.dtos.response.order.CategoryPricingDetailResponse;
import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.service.entityServices.order.order.CategoryPricingDetailEntityService;
import capstone_project.service.mapper.order.CategoryPricingDetailMapper;
import capstone_project.service.services.order.category.CategoryPricingDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryPricingDetailServiceImpl implements CategoryPricingDetailService {

    private final CategoryPricingDetailEntityService categoryPricingDetailEntityService;
    private final CategoryPricingDetailMapper categoryPricingDetailMapper;

    @Override
    public List<CategoryPricingDetailResponse> getAllCategoryPricingDetails() {
        log.info("Fetching all category pricing details");
        List<CategoryPricingDetailEntity> categoryPricingDetailEntities = categoryPricingDetailEntityService.findAll();
        if (categoryPricingDetailEntities.isEmpty()) {
            log.warn("No category pricing details found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return categoryPricingDetailEntities.stream()
                .map(categoryPricingDetailMapper::toCategoryPricingDetailResponse)
                .toList();
    }

    @Override
    public CategoryPricingDetailResponse getCategoryPricingDetailById(UUID id) {
        log.info("Fetching category pricing detail by ID: {}", id);
        CategoryPricingDetailEntity categoryPricingDetailEntity = categoryPricingDetailEntityService.findContractRuleEntitiesById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return categoryPricingDetailMapper.toCategoryPricingDetailResponse(categoryPricingDetailEntity);
    }

    @Override
    public CategoryPricingDetailResponse createCategoryPricingDetail(CategoryPricingDetailRequest categoryPricingDetailRequest) {
        log.info("Creating new category pricing detail");
        if (categoryPricingDetailRequest.categoryId() == null) {
            log.error("Category ID is required for creating a category pricing detail");
            throw new IllegalArgumentException("Category ID must not be null");
        }

        UUID categoryUuid = UUID.fromString(categoryPricingDetailRequest.categoryId());
        CategoryPricingDetailEntity existingEntity = categoryPricingDetailEntityService.findByCategoryId(categoryUuid);

        if (existingEntity != null) {
            log.error("Category pricing detail already exists for category ID: {}", categoryUuid);
            throw new BadRequestException(
                    ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        CategoryPricingDetailEntity categoryPricingDetailEntity = categoryPricingDetailMapper.mapRequestToEntity(categoryPricingDetailRequest);
        CategoryPricingDetailEntity savedEntity = categoryPricingDetailEntityService.save(categoryPricingDetailEntity);
        return categoryPricingDetailMapper.toCategoryPricingDetailResponse(savedEntity);
    }

    @Override
    public CategoryPricingDetailResponse updateCategoryPricingDetail(UUID id, CategoryPricingDetailRequest categoryPricingDetailRequest) {
        log.info("Updating category pricing detail with ID: {}", id);
        CategoryPricingDetailEntity existingEntity = categoryPricingDetailEntityService.findContractRuleEntitiesById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        categoryPricingDetailMapper.toCategoryPricingDetailEntity(categoryPricingDetailRequest, existingEntity);
        CategoryPricingDetailEntity savedEntity = categoryPricingDetailEntityService.save(existingEntity);

        return categoryPricingDetailMapper.toCategoryPricingDetailResponse(savedEntity);
    }

    @Override
    public void deleteCategory(UUID id) {

    }
}
