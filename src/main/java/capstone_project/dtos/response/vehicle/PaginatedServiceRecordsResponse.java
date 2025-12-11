package capstone_project.dtos.response.vehicle;

import java.util.List;

public record PaginatedServiceRecordsResponse(
    List<VehicleServiceRecordResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
