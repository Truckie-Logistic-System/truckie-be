package capstone_project.service.mapper.user;

import capstone_project.dtos.request.user.AddressRequest;
import capstone_project.dtos.response.user.AddressResponse;
import capstone_project.entity.user.address.AddressEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    @Mapping(target = "id", source = "address.id")
    @Mapping(target = "customerId", source = "address.customer.id", 
             nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    AddressResponse toAddressResponse(AddressEntity address);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    AddressEntity mapRequestToAddressEntity(AddressRequest addressRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    void toAddressEntity(AddressRequest request, @MappingTarget AddressEntity entity);
}