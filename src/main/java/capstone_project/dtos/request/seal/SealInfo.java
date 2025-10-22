package capstone_project.dtos.request.seal;

/**
 * DTO for seal information in vehicle assignment request
 * Contains seal code and description
 */
public record SealInfo(
        String sealCode,
        String description
) {}
