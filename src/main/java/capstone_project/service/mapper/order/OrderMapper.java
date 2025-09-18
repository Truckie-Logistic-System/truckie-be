package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.dtos.response.order.GetOrderForGetAllResponse;
import capstone_project.dtos.response.order.GetOrderResponse;
import capstone_project.dtos.response.order.OrderForCustomerListResponse;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface OrderMapper {

    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "deliveryAddress.id", target = "deliveryId")
    @Mapping(source = "pickupAddress.id", target = "pickupAddressId")
    @Mapping(source = "category.id", target = "categoryId")
    CreateOrderResponse toCreateOrderResponse(OrderEntity entity);

    List<CreateOrderResponse> toCreateOrderResponses(List<OrderEntity> orderEntities);

    @Mapping(source = "orderDetailEntities", target = "orderDetails")
    GetOrderResponse toGetOrderResponse(OrderEntity entity);

    @Mapping(source = "pickupAddress.id", target = "pickupAddressId")
    @Mapping(source = "category.id", target = "categoryId")
    GetOrderForGetAllResponse toGetOrderForGetAllResponse(OrderEntity entity);

    List<GetOrderForGetAllResponse> toGetOrderForGetAllResponses(List<OrderEntity> orderEntities);

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
