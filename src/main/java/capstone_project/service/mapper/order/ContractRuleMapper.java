package capstone_project.service.mapper.order;

import capstone_project.dtos.request.order.ContractRuleRequest;
import capstone_project.dtos.response.order.contract.ContractRuleResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ContractRuleMapper {

    @Mapping(source = "contractEntity.id", target = "contractEntityId")
    @Mapping(source = "sizeRuleEntity.id", target = "sizeRuleId")
    ContractRuleResponse toContractRuleResponse(ContractRuleEntity contractRuleEntity);

    @Mapping(source = "sizeRuleId", target = "sizeRuleEntity", qualifiedByName = "sizeRuleFromId")
    @Mapping(source = "contractEntityId", target = "contractEntity", qualifiedByName = "contractFromId")
    ContractRuleEntity mapRequestToEntity(ContractRuleRequest contractRuleRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "sizeRuleId", target = "sizeRuleEntity", qualifiedByName = "sizeRuleFromId")
    @Mapping(source = "contractEntityId", target = "contractEntity", qualifiedByName = "contractFromId")
    void toContractRuleEntity(ContractRuleRequest request, @MappingTarget ContractRuleEntity entity);

    @Named("sizeRuleFromId")
    default SizeRuleEntity mapSizeRuleFromId(String sizeRuleId) {
        SizeRuleEntity entity = new SizeRuleEntity();
        entity.setId(UUID.fromString(sizeRuleId));
        return entity;
    }

    @Named("contractFromId")
    default ContractEntity mapContractFromId(String contractId) {
        ContractEntity entity = new ContractEntity();
        entity.setId(UUID.fromString(contractId));
        return entity;
    }

}
