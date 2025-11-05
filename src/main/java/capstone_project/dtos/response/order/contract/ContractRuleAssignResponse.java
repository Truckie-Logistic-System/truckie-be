package capstone_project.dtos.response.order.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContractRuleAssignResponse {
    private int vehicleIndex;
    private UUID sizeRuleId;
    //    private int numOfVehicles;
//    private Integer numOfVehicles;
    private String sizeRuleName;
    private BigDecimal currentLoad;
    private String currentLoadUnit; // Đơn vị của currentLoad (luôn là "Tấn")
    //    private List<UUID> assignedDetails = new ArrayList<>();
    private List<OrderDetailForPackingResponse> assignedDetails = new ArrayList<>();
    List<PackedDetailResponse> packedDetailDetails;

}



