package capstone_project.dtos.request.order;

import java.util.List;
import java.util.UUID;

public record DetailsForAssignemntRequest(
        List<UUID> details
) {
}
