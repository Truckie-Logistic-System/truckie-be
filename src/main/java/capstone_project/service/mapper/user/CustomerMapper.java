package capstone_project.service.mapper.user;

import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.entity.user.customer.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "user", target = "userResponse")
    CustomerResponse mapCustomerResponse(final CustomerEntity customerEntity);
}
