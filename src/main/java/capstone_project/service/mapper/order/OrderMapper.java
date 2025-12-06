package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.*;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class, VehicleAssignmentMapper.class})
public interface OrderMapper {

    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "deliveryAddress.id", target = "deliveryId")
    @Mapping(source = "pickupAddress.id", target = "pickupAddressId")
    @Mapping(source = "category.id", target = "categoryId")
    CreateOrderResponse toCreateOrderResponse(OrderEntity entity);

    List<CreateOrderResponse> toCreateOrderResponses(List<OrderEntity> orderEntities);

    @Mapping(source = "orderDetailEntities", target = "orderDetails")
    @Mapping(source = "hasInsurance", target = "hasInsurance")
    @Mapping(source = "totalInsuranceFee", target = "totalInsuranceFee")
    @Mapping(source = "totalDeclaredValue", target = "totalDeclaredValue")
    @Mapping(target = "vehicleAssignments", expression = "java(extractUniqueVehicleAssignments(entity))")
    GetOrderResponse toGetOrderResponse(OrderEntity entity);

    default List<VehicleAssignmentResponse> extractUniqueVehicleAssignments(OrderEntity entity) {
        if (entity == null || entity.getOrderDetailEntities() == null) {
            return List.of();
        }
        return entity.getOrderDetailEntities().stream()
                .map(orderDetail -> orderDetail.getVehicleAssignmentEntity())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(this::toVehicleAssignmentResponse)
                .collect(Collectors.toList());
    }

    default VehicleAssignmentResponse toVehicleAssignmentResponse(VehicleAssignmentEntity entity) {
        if (entity == null) return null;
        
        // Map vehicle info
        VehicleAssignmentResponse.VehicleInfo vehicleInfo = null;
        if (entity.getVehicleEntity() != null) {
            var vehicle = entity.getVehicleEntity();
            var vehicleType = vehicle.getVehicleTypeEntity() != null
                ? new VehicleAssignmentResponse.VehicleTypeInfo(
                    vehicle.getVehicleTypeEntity().getId(),
                    vehicle.getVehicleTypeEntity().getVehicleTypeName(),
                    vehicle.getVehicleTypeEntity().getDescription()
                )
                : null;
                
            vehicleInfo = new VehicleAssignmentResponse.VehicleInfo(
                vehicle.getId(),
                vehicle.getLicensePlateNumber(),
                vehicle.getModel(),
                vehicle.getManufacturer(),
                vehicle.getYear(),
                vehicleType
            );
        }
        
        // Map driver 1
        VehicleAssignmentResponse.DriverInfo driver1 = null;
        if (entity.getDriver1() != null) {
            var d1 = entity.getDriver1();
            driver1 = new VehicleAssignmentResponse.DriverInfo(
                d1.getId(),
                d1.getUser() != null ? d1.getUser().getFullName() : "N/A",
                d1.getUser() != null ? d1.getUser().getPhoneNumber() : null,
                d1.getDriverLicenseNumber(),
                d1.getLicenseClass(),
                null
            );
        }
        
        // Map driver 2
        VehicleAssignmentResponse.DriverInfo driver2 = null;
        if (entity.getDriver2() != null) {
            var d2 = entity.getDriver2();
            driver2 = new VehicleAssignmentResponse.DriverInfo(
                d2.getId(),
                d2.getUser() != null ? d2.getUser().getFullName() : "N/A",
                d2.getUser() != null ? d2.getUser().getPhoneNumber() : null,
                d2.getDriverLicenseNumber(),
                d2.getLicenseClass(),
                null
            );
        }
        
        return new VehicleAssignmentResponse(
                entity.getId(),
                entity.getVehicleEntity() != null ? entity.getVehicleEntity().getId() : null,
                entity.getDriver1() != null ? entity.getDriver1().getId() : null,
                entity.getDriver2() != null ? entity.getDriver2().getId() : null,
                entity.getStatus(),
                entity.getTrackingCode(),
                vehicleInfo,
                driver1,
                driver2
        );
    }

    @Mapping(source = "pickupAddress.id", target = "pickupAddressId")
    @Mapping(source = "category.id", target = "categoryId")
    GetOrderForGetAllResponse toGetOrderForGetAllResponse(OrderEntity entity);

    GetOrderForDriverResponse toGetOrderForDriverResponse(OrderEntity entity);

    List<GetOrderForGetAllResponse> toGetOrderForGetAllResponses(List<OrderEntity> orderEntities);

    @Mapping(target = "orderDetailEntities", source = "orderDetailEntities")
    GetOrderByJpaResponse toGetOrderByJpaResponse(OrderEntity entity);
    
    // Custom mapping for nested OrderDetailEntity to GetOrderDetailByJpaResponse
    @Mapping(source = "weightTons", target = "weight")
    GetOrderDetailByJpaResponse toGetOrderDetailByJpaResponse(capstone_project.entity.order.order.OrderDetailEntity orderDetail);

    @Mapping(target = "pickupAddress", expression = "java(formatAddress(entity.getPickupAddress()))")
    @Mapping(target = "deliveryAddress", expression = "java(formatAddress(entity.getDeliveryAddress()))")
    @Mapping(source = "deliveryAddress.id", target = "deliveryAddressId")
    OrderForCustomerListResponse toOrderForCustomerListResponse(OrderEntity entity);

    List<OrderForCustomerListResponse> toOrderForCustomerListResponses(List<OrderEntity> orderEntities);

    default String formatAddress(AddressEntity address) {
        if (address == null) {
            return "";
        }
        return String.format("%s, %s, %s",
                address.getStreet(),
                address.getWard(),
                address.getProvince());
    }

//    @Mapping(source = "deliveryAddress.id", target = "deliveryAddressId")
//    @Mapping(source = "pickupAddress.id", target = "pickupAddressId")
//    @Mapping(source = "sender.id", target = "senderId")
//    GetOrderResponse toOrderResponse(final OrderEntity orderEntity);
//
//    @Mapping(source = "deliveryAddressId", target = "deliveryAddress", qualifiedByName = "deliveryAddressFromId")
//    @Mapping(source = "pickupAddressId", target = "pickupAddress", qualifiedByName = "pickupAddressFromId")
//    @Mapping(source = "senderId", target = "sender", qualifiedByName = "senderFromId")
//    OrderEntity mapRequestToEntity(final OrderRequest orderRequest);
//
//    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(target = "deliveryAddress", source = "deliveryAddressId", qualifiedByName = "deliveryAddressFromId")
//    @Mapping(target = "pickupAddress", source = "pickupAddressId", qualifiedByName = "pickupAddressFromId")
//    @Mapping(target = "sender", source = "senderId", qualifiedByName = "senderFromId")
//    void toOrderEntity(OrderRequest request, @MappingTarget OrderEntity entity);
//
//    @Named("deliveryAddressFromId")
//    default AddressEntity mapDeliveryAddressFromId(String addressId) {
//        AddressEntity entity = new AddressEntity();
//        entity.setId(UUID.fromString(addressId));
//        return entity;
//    }
//
//    @Named("pickupAddressFromId")
//    default AddressEntity mapPickupAddressFromId(String addressId) {
//        AddressEntity entity = new AddressEntity();
//        entity.setId(UUID.fromString(addressId));
//        return entity;
//    }
//
//    @Named("senderFromId")
//    default CustomerEntity mapSenderFromId(String senderId) {
//        CustomerEntity entity = new CustomerEntity();
//        entity.setId(UUID.fromString(senderId));
//        return entity;
//    }

}
