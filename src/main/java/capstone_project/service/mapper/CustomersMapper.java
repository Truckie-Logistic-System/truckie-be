package capstone_project.service.mapper;

import capstone_project.controller.dtos.response.CustomerResponse;
import capstone_project.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomersMapper {

    @Mapping(source = "user", target = "userResponse")
    CustomerResponse mapCustomerResponse(final CustomerEntity customerEntity);
}
