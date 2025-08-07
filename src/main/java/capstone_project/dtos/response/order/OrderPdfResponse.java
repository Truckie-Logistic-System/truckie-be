package capstone_project.dtos.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPdfResponse {
    private String pdfUrl;
    private String orderId;
    private String message;
}