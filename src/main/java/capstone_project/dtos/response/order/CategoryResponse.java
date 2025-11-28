package capstone_project.dtos.response.order;

import capstone_project.common.enums.CategoryName;

public record CategoryResponse(
    String id,
    CategoryName categoryName,
    String description
) {
}
