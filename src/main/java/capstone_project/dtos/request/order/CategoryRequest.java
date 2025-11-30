package capstone_project.dtos.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Category name cannot be blank")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String categoryName,
        
        @Size(max = 200, message = "Description must not exceed 200 characters")
        String description
) {
}
