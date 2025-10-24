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
        return new VehicleAssignmentResponse(
                entity.getId(),
                entity.getVehicleEntity() != null ? entity.getVehicleEntity().getId() : null,
                entity.getDriver1() != null && entity.getDriver1().getUser() != null ? entity.getDriver1().getUser().getId() : null,
                entity.getDriver2() != null && entity.getDriver2().getUser() != null ? entity.getDriver2().getUser().getId() : null,
                entity.getStatus(),
                entity.getTrackingCode()
        );
    }

    @Mapping(source = "pickupAddress.id", target = "pickupAddressId")
    @Mapping(source = "category.id", target = "categoryId")
    GetOrderForGetAllResponse toGetOrderForGetAllResponse(OrderEntity entity);

    GetOrderForDriverResponse toGetOrderForDriverResponse(OrderEntity entity);

    List<GetOrderForGetAllResponse> toGetOrderForGetAllResponses(List<OrderEntity> orderEntities);

    GetOrderByJpaResponse toGetOrderByJpaResponse(OrderEntity entity);

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
