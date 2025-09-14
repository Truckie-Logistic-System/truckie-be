package capstone_project.service.mapper.order;

import capstone_project.dtos.request.order.ContractRuleRequest;
import capstone_project.dtos.response.order.contract.ContractRuleResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ContractRuleMapper {

    @Mapping(source = "contractEntity.id", target = "contractEntityId")
    @Mapping(source = "vehicleRuleEntity.id", target = "vehicleRuleId")
    ContractRuleResponse toContractRuleResponse(ContractRuleEntity contractRuleEntity);

    @Mapping(source = "vehicleRuleId", target = "vehicleRuleEntity", qualifiedByName = "vehicleRuleFromId")
    @Mapping(source = "contractEntityId", target = "contractEntity", qualifiedByName = "contractFromId")
    ContractRuleEntity mapRequestToEntity(ContractRuleRequest contractRuleRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "vehicleRuleId", target = "vehicleRuleEntity", qualifiedByName = "vehicleRuleFromId")
    @Mapping(source = "contractEntityId", target = "contractEntity", qualifiedByName = "contractFromId")
    void toContractRuleEntity(ContractRuleRequest request, @MappingTarget ContractRuleEntity entity);

    @Named("vehicleRuleFromId")
    default VehicleRuleEntity mapVehicleRuleFromId(String vehicleRuleId) {
        VehicleRuleEntity entity = new VehicleRuleEntity();
        entity.setId(UUID.fromString(vehicleRuleId));
        return entity;
    }

    @Named("contractFromId")
    default ContractEntity mapContractFromId(String contractId) {
        ContractEntity entity = new ContractEntity();
        entity.setId(UUID.fromString(contractId));
        return entity;
    }

}
