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
    private UUID vehicleRuleId;
    //    private int numOfVehicles;
//    private Integer numOfVehicles;
    private String vehicleRuleName;
    private BigDecimal currentLoad;
    //    private List<UUID> assignedDetails = new ArrayList<>();
    private List<OrderDetailForPackingResponse> assignedDetails = new ArrayList<>();

}



