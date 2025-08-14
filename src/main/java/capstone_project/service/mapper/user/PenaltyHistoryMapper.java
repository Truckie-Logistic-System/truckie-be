package capstone_project.service.mapper.user;

import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface PenaltyHistoryMapper {

    // Entity ➜ Response
    @Mapping(source="issueBy.id", target="driverId")
    @Mapping(source="vehicleAssignmentEntity.id", target="vehicleAssignmentId")
    PenaltyHistoryResponse toPenaltyHistoryResponse(PenaltyHistoryEntity entity);

    // Request ➜ Entity (CREATE)
    @Mapping(target="id", ignore=true)
    @Mapping(target="createdAt", ignore=true)
    @Mapping(target="modifiedAt", ignore=true)
    @Mapping(target="issueBy.id",            source="driverId")
    @Mapping(target="vehicleAssignmentEntity.id", source="vehicleAssignmentId")
    PenaltyHistoryEntity toEntity(PenaltyHistoryRequest req);

    // UPDATE patch (MapStruct “merge”)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target="issueBy.id",            source="driverId")
    @Mapping(target="vehicleAssignmentEntity.id", source="vehicleAssignmentId")
    void toEntity(PenaltyHistoryRequest req,
                  @MappingTarget PenaltyHistoryEntity entity);
}