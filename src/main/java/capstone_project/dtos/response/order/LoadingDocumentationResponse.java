package capstone_project.dtos.response.order;

import capstone_project.dtos.response.order.seal.GetSealResponse;
import java.util.List;
import java.util.UUID;

public record LoadingDocumentationResponse(
    UUID vehicleAssignmentId,
    List<PackingProofImageResponse> packingProofImages,
    GetSealResponse sealInformation
) {
}
