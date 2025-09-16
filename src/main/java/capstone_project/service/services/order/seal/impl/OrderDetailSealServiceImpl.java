package capstone_project.service.services.order.seal.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.seal.OrderDetailSealRequest;
import capstone_project.dtos.response.order.seal.GetOrderDetailSealResponse;
import capstone_project.dtos.response.order.seal.GetSealFullResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderDetailSealEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailSealEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.service.mapper.order.OrderDetailSealMapper;
import capstone_project.service.mapper.order.SealMapper;
import capstone_project.service.services.order.seal.OrderDetailSealService;
import capstone_project.service.services.order.seal.SealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailSealServiceImpl implements OrderDetailSealService {
    private final OrderDetailSealEntityService  orderDetailSealEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final SealEntityService sealEntityService;
    private final SealService sealService;
    private final OrderDetailSealMapper orderDetailSealMapper;
    private final SealMapper sealMapper;

    @Override
    @Transactional
    public GetSealFullResponse assignAFirstSealForOrderDetail(OrderDetailSealRequest orderDetailSealRequest) {

        List<OrderDetailSealEntity> orderDetailSealEntities = new ArrayList<>();

        // Lấy tất cả orderDetails một lần
        List<OrderDetailEntity> orderDetails = orderDetailSealRequest.orderDetails().stream()
                .map(id -> orderDetailEntityService.findEntityById(id)
                        .orElseThrow(() -> new NotFoundException(
                                "Không tìm thấy detail với ID: " + id,
                                ErrorEnum.NOT_FOUND.getErrorCode())))
                .toList();

        // Kiểm tra nếu đã có seal active
        for (OrderDetailEntity orderDetail : orderDetails) {
            OrderDetailSealEntity existingSeal = orderDetailSealEntityService.findByOrderDetail(orderDetail,CommonStatusEnum.ACTIVE.name());
            if (existingSeal != null ) {
                throw new BadRequestException(
                        "Seal này vẫn đang active không thể tạo Seal mới, xóa seal cũ trước khi tạo seal mới cho từng đơn hàng",
                        ErrorEnum.INVALID_REQUEST.getErrorCode());
            }
        }

        // Tạo mới Seal
        GetSealResponse getSealResponse = sealService.createSeal(orderDetailSealRequest.description());

        // Gán seal vừa tạo cho từng orderDetail
        for (OrderDetailEntity orderDetail : orderDetails) {
            OrderDetailSealEntity orderDetailSealEntity = OrderDetailSealEntity.builder()
                    .description(orderDetailSealRequest.description())
                    .sealDate(orderDetailSealRequest.sealDate())
                    .status(CommonStatusEnum.ACTIVE.name())
                    .seal(sealMapper.toSealEntity(getSealResponse))
                    .orderDetail(orderDetail)
                    .build();
            orderDetailSealEntities.add(orderDetailSealEntity);
        }

        orderDetailSealEntityService.saveAll(orderDetailSealEntities);

        return new GetSealFullResponse(
                getSealResponse,
                orderDetailSealMapper.toGetOrderDetailSealResponses(orderDetailSealEntities)
        );
    }


    @Override
    public GetSealFullResponse removeSealForDetailsBySealId(UUID sealId) {
        List<OrderDetailSealEntity> getOrderDetailSeals = new ArrayList<>();
        Optional<SealEntity> seal = sealEntityService.findEntityById(sealId);
        if(seal.isEmpty()){
            throw new NotFoundException("Không tìm thấy seal với ID: "+sealId, ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<OrderDetailSealEntity> getOrderDetailSealEntities = orderDetailSealEntityService.findBySeal(seal.get());
        for(OrderDetailSealEntity orderDetailSealEntity : getOrderDetailSealEntities){
            orderDetailSealEntity.setStatus(CommonStatusEnum.DELETED.name());
            getOrderDetailSeals.add(orderDetailSealEntity);
        }
        orderDetailSealEntityService.saveAll(getOrderDetailSeals);
        seal.get().setStatus(CommonStatusEnum.DELETED.name());

        return new GetSealFullResponse(
                sealMapper.toGetSealResponse(seal.get()),
                orderDetailSealMapper.toGetOrderDetailSealResponses(getOrderDetailSeals)
        );
    }

    @Override
    public GetSealFullResponse getAllBySealId(UUID sealId) {
        Optional<SealEntity> seal = sealEntityService.findEntityById(sealId);
        if(seal.isEmpty()){
            throw new NotFoundException("Không tìm thấy seal với ID: "+sealId, ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<OrderDetailSealEntity> getOrderDetailSealEntities = orderDetailSealEntityService.findBySeal(seal.get());

        return new GetSealFullResponse(
                sealMapper.toGetSealResponse(seal.get()),
                orderDetailSealMapper.toGetOrderDetailSealResponses(getOrderDetailSealEntities)
        );
    }

    @Override
    public GetOrderDetailSealResponse getActiveOrderSealByOrderDetailId(UUID orderDetailId) {
        Optional<OrderDetailEntity> orderDetail = orderDetailEntityService.findEntityById(orderDetailId);
        if(orderDetail.isEmpty()){
            throw new NotFoundException("Không tìm thấy chi tiết đơn hàng với ID: "+orderDetailId, ErrorEnum.NOT_FOUND.getErrorCode());
        }
        OrderDetailSealEntity existingSeal = orderDetailSealEntityService.findByOrderDetail(orderDetail.get(),CommonStatusEnum.ACTIVE.name());
        return orderDetailSealMapper.toGetOrderDetailSealResponse(existingSeal);
    }


}
