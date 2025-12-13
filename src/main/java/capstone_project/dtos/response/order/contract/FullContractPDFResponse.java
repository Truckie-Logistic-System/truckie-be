package capstone_project.dtos.response.order.contract;

import capstone_project.dtos.response.order.GetOrderResponse;
import capstone_project.dtos.response.setting.CarrierSettingResponse;
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

        CarrierSettingResponse carrierInfo,
        CustomerResponse customerInfo,
        GetOrderResponse orderInfo,
        PriceCalculationResponse priceDetails,
        List<ContractRuleAssignResponse> assignResult,
        ContractSettingResponse contractSettings,
        // Custom deposit percent for this contract (overrides contractSettings.depositPercent if set)
        BigDecimal customDepositPercent
) {
}
