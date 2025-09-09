package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.transaction.GetTransactionStatusResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

//    @Mapping(target = "contractEntity", source = "contract")
//    TransactionEntity toEntity(TransactionResponse response, ContractEntity contract);

    @Mapping(source = "contractEntity.id", target = "contractId")
    TransactionResponse toTransactionResponse(final TransactionEntity basingPriceEntity);

    GetTransactionStatusResponse toGetTransactionStatusResponse(final TransactionEntity transactionEntity);

//    @Mapping(target = "contractEntity", source = "contractId", qualifiedByName = "contractFromId")
//    TransactionEntity mapRequestToEntity(final TransactionRequest transactionRequest);

    @Named("contractFromId")
    default ContractEntity mapContractFromId(String contractId) {
        ContractEntity entity = new ContractEntity();
        entity.setId(UUID.fromString(contractId));
        return entity;
    }
}
