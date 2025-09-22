package capstone_project.dtos.request.order;

import java.util.List;

public record DetailsForAssignemntRequest(
        List<String> trackingCodes
) {
}
