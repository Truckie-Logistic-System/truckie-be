package capstone_project.dtos.response.order.contract;

import capstone_project.dtos.response.order.GetOrderResponse;
import capstone_project.dtos.response.setting.ContractSettingResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record FullContractPDFResponse(
        String pdfUrl,
        String contractId,
        String message,

        BigDecimal distanceKm,


        CustomerResponse customerInfo,
//        UserResponse senderInfo,
        GetOrderResponse orderInfo,
        PriceCalculationResponse priceDetails,
        List<ContractRuleAssignResponse> assignResult,
        ContractSettingResponse contractSettings
) {
}
