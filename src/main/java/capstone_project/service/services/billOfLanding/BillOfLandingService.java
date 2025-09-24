package capstone_project.service.services.billOfLanding;

import capstone_project.dtos.response.order.BillOfLandingResponse;

import java.util.Map;
import java.util.UUID;

public interface BillOfLandingService {
    BillOfLandingResponse getBillOfLandingById(UUID contractId);

    Map<String, byte[]> generateBillOfLadingAndCargoManifests(UUID orderId);
}
