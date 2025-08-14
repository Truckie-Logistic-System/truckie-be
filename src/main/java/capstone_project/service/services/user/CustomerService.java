package capstone_project.service.services.user;

import java.util.UUID;

public interface CustomerService {

    void updateCustomerStatus(UUID userId, String status);
}
