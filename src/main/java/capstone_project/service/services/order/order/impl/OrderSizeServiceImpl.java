package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CreateOrderSizeRequest;
import capstone_project.dtos.request.order.UpdateOrderSizeRequest;
import capstone_project.dtos.response.order.GetOrderSizeResponse;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.repository.entityServices.order.order.OrderSizeEntityService;
import capstone_project.service.mapper.order.OrderSizeMapper;
import capstone_project.service.services.order.order.OrderSizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSizeServiceImpl implements OrderSizeService {
    private final OrderSizeEntityService orderSizeEntityService;
    private final OrderSizeMapper orderSizeMapper;

    @Override
    public GetOrderSizeResponse createOrderSize(CreateOrderSizeRequest request) {
        if(request == null){
            log.error("CreateOrderSizeRequest is null");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + "CreateOrderSizeRequest cannot be null",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        if (request.maxHeight().compareTo(request.minHeight()) < 0
//                || request.maxWeight().compareTo(request.minWeight()) < 0
                || request.maxLength().compareTo(request.minLength()) < 0
                || request.maxWidth().compareTo(request.minWidth()) < 0) {
            throw new BadRequestException(
                    ErrorEnum.INVALID_REQUEST.getMessage() + "Giá trị max không được nhỏ hơn min",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        try{
            OrderSizeEntity orderSizeEntity = OrderSizeEntity.builder()
                    .description(request.description())
                    .status(CommonStatusEnum.ACTIVE.name())
                    .minHeight(request.minHeight())
//                    .minWeight(request.minWeight())
                    .maxHeight(request.maxHeight())
//                    .maxWeight(request.maxWeight())
                    .maxLength(request.maxLength())
                    .minLength(request.minLength())
                    .maxWidth(request.maxWidth())
                    .minWidth(request.minWidth())
                    .build();
            return orderSizeMapper.toOrderSizeResponse(orderSizeEntityService.save(orderSizeEntity));
        }catch (IllegalArgumentException e){
            log.error("[CreateOrderSizeRequest] Invalid CreateOrderSizeRequest format");
            throw e;
        }

    }

    @Override
    public GetOrderSizeResponse updateOrderSize(UpdateOrderSizeRequest request) {
        if(request == null){
            log.error("UpdateOrderSizeRequest is null");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + "UpdateOrderSizeRequest cannot be null",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        OrderSizeEntity orderSizeEntity = orderSizeEntityService.findEntityById(UUID.fromString(request.id()))
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " orderSizeEntity with ID: " + request.id(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (request.maxHeight().compareTo(request.minHeight()) < 0
//                || request.maxWeight().compareTo(request.minWeight()) < 0
                || request.maxLength().compareTo(request.minLength()) < 0
                || request.maxWidth().compareTo(request.minWidth()) < 0) {
            throw new BadRequestException(
                    ErrorEnum.INVALID_REQUEST.getMessage() + "Giá trị max không được nhỏ hơn min",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        try{
            OrderSizeEntity orderSizeEntityUpdate = OrderSizeEntity.builder()
                    .id(orderSizeEntity.getId())
                    .description(request.description())
                    .status(orderSizeEntity.getStatus())
                    .minHeight(request.minHeight())
//                    .minWeight(request.minWeight())
                    .maxHeight(request.maxHeight())
//                    .maxWeight(request.maxWeight())
                    .maxLength(request.maxLength())
                    .minLength(request.minLength())
                    .maxWidth(request.maxWidth())
                    .minWidth(request.minWidth())
                    .build();
            return orderSizeMapper.toOrderSizeResponse(orderSizeEntityService.save(orderSizeEntityUpdate));
        }catch (IllegalArgumentException e){
            log.error("[UpdateOrderSizeRequest] Invalid UpdateOrderSizeRequest format");
            throw e;
        }

    }

    @Override
    public boolean deleteOrderSize(UUID id) {
        var entity = orderSizeEntityService.findEntityById(id)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " orderSizeEntity with ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        entity.setStatus(CommonStatusEnum.DELETED.name());
        return orderSizeEntityService.save(entity) != null;
    }

    @Override
    public GetOrderSizeResponse getOrderSizeById(UUID id) {
        var entity = orderSizeEntityService.findEntityById(id)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " orderSizeEntity with ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return orderSizeMapper.toOrderSizeResponse(entity);
    }

    @Override
    public List<GetOrderSizeResponse> getAllOrderSizes() {
        var entity = orderSizeEntityService.findAll();

        return orderSizeMapper.toOrderSizeResponseList(entity);
    }

    @Override
    public boolean activeOrderSize(UUID id) {
        var entity = orderSizeEntityService.findEntityById(id)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " orderSizeEntity with ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        entity.setStatus(CommonStatusEnum.ACTIVE.name());
        return orderSizeEntityService.save(entity) != null;
    }
}
