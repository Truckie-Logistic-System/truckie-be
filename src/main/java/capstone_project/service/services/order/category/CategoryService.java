package capstone_project.service.services.order.category;

import capstone_project.dtos.request.order.CategoryRequest;
import capstone_project.dtos.response.order.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getAllCategoriesByCategoryName(String categoryName);

    CategoryResponse getCategoryById(UUID id);

    CategoryResponse getCategoryByName(String categoryName);

    CategoryResponse createCategory(CategoryRequest categoryRequest);

    CategoryResponse updateCategory(UUID id, CategoryRequest categoryRequest);

    void deleteCategory(UUID id);
}
