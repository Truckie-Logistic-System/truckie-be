package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private String message;
    private String sessionId;
    private List<SuggestedAction> suggestedActions;
    private PriceEstimateData priceEstimate; // Nếu AI tính phí

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuggestedAction {
        private String label;
        private String action;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PriceEstimateData {
        private Double distance;
        private Double weight;
        private String vehicleType;
        private Double estimatedPrice;
        private String breakdown;
    }
}
