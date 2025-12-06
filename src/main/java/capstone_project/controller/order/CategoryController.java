package capstone_project.controller.order;

import capstone_project.dtos.request.order.CategoryRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.CategoryResponse;
import capstone_project.service.services.order.category.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${category.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        final var result = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{categoryName}/list")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesByName(@PathVariable String categoryName) {
        final var result = categoryService.getAllCategoriesByCategoryName(categoryName);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable("id") UUID id) {
        final var result = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryByName(@RequestParam String categoryName) {
        final var result = categoryService.getCategoryByName(categoryName);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @PostMapping("")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody @Valid CategoryRequest categoryRequest) {
        final var result = categoryService.createCategory(categoryRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable("id") UUID id,
                                                                        @RequestBody @Valid CategoryRequest categoryRequest) {
        final var result = categoryService.updateCategory(id, categoryRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable("id") UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted successfully"));
    }
}
