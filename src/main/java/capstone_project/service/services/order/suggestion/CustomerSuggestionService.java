package capstone_project.service.services.order.suggestion;

import capstone_project.dtos.response.order.ReceiverDetailResponse;
import capstone_project.dtos.response.order.ReceiverSuggestionResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerSuggestionService {
    /**
     * Get recent receivers for the current customer
     * @param limit maximum number of receivers to return
     * @return list of receiver suggestions
     */
    List<ReceiverSuggestionResponse> getRecentReceivers(int limit);

    /**
     * Get detailed information about a receiver from a specific order
     * @param orderId the ID of the order
     * @return detailed recipient information with addresses
     */
    ReceiverDetailResponse getReceiverDetails(UUID orderId);
}
