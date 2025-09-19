package capstone_project.service.services.billOfLanding;

import capstone_project.dtos.response.order.BillOfLandingResponse;

import java.util.UUID;

public interface BillOfLandingService {
    BillOfLandingResponse getBillOfLandingById(UUID contractId);
}
