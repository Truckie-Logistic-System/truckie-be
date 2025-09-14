package capstone_project.service.mapper.user;

import capstone_project.dtos.request.user.UpdateCustomerRequest;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.entity.user.customer.CustomerEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "user", target = "userResponse")
    CustomerResponse mapCustomerResponse(final CustomerEntity customerEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toCustomerEntity(UpdateCustomerRequest request, @MappingTarget CustomerEntity entity);
}
