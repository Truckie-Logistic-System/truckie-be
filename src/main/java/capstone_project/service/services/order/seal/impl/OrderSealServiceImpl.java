package capstone_project.service.services.order.seal.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.seal.OrderSealRequest;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.dtos.response.order.seal.GetSealFullResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.entity.order.order.OrderSealEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.order.OrderSealEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.order.OrderSealMapper;
import capstone_project.service.mapper.order.SealMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.seal.OrderSealService;
import capstone_project.service.services.order.seal.SealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSealServiceImpl implements OrderSealService {
    private final OrderSealEntityService orderSealEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final SealEntityService sealEntityService;
    private final SealService sealService;
    private final OrderSealMapper orderSealMapper;
    private final SealMapper sealMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public GetSealFullResponse assignSealForVehicleAssignment(OrderSealRequest orderSealRequest) {
        List<OrderSealEntity> orderSealEntities = new ArrayList<>();

        // Lấy vehicle assignment
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(orderSealRequest.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + orderSealRequest.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        // Kiểm tra nếu đã có seal active
        OrderSealEntity existingSeal = orderSealEntityService.findByVehicleAssignment(vehicleAssignment, CommonStatusEnum.ACTIVE.name());
        if (existingSeal != null) {
            throw new BadRequestException(
                    "Seal này vẫn đang active không thể tạo Seal mới, xóa seal cũ trước khi tạo seal mới cho chuyến đi",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Tự động tạo mô tả
        String description = "Seal được tạo cho chuyến " + vehicleAssignment.getTrackingCode();

        // Tạo mới Seal
        GetSealResponse getSealResponse = sealService.createSeal(description);

        // Upload hình ảnh và lấy URL
        String imageUrl = null;
        try {
            MultipartFile sealImage = orderSealRequest.sealImage();
            String fileName = "seal_" + UUID.randomUUID();
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                    sealImage.getBytes(),
                    fileName,
                    "seals" // folder name on Cloudinary
            );

            // Lấy URL của ảnh đã upload
            imageUrl = cloudinaryService.getFileUrl((String) uploadResult.get("public_id"));
        } catch (IOException e) {
            log.error("Lỗi khi upload ảnh seal: {}", e.getMessage(), e);
            throw new BadRequestException(
                    "Không thể upload ảnh seal: " + e.getMessage(),
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Tạo OrderSealEntity với ảnh đã upload
        OrderSealEntity orderSealEntity = OrderSealEntity.builder()
                .description(description)
                .sealDate(LocalDateTime.now()) // Tự động set thời gian hiện tại
                .sealAttachedImage(imageUrl) // Lưu URL của ảnh
                .status(CommonStatusEnum.ACTIVE.name())
                .seal(sealMapper.toSealEntity(getSealResponse))
                .vehicleAssignment(vehicleAssignment)
                .build();

        orderSealEntities.add(orderSealEntity);
        orderSealEntityService.saveAll(orderSealEntities);

        return new GetSealFullResponse(
                getSealResponse,
                orderSealMapper.toGetOrderSealResponses(orderSealEntities)
        );
    }

    @Override
    public GetSealFullResponse removeSealBySealId(UUID sealId) {
        List<OrderSealEntity> orderSeals = new ArrayList<>();
        Optional<SealEntity> seal = sealEntityService.findEntityById(sealId);
        if (seal.isEmpty()) {
            throw new NotFoundException("Không tìm thấy seal với ID: " + sealId, ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<OrderSealEntity> orderSealEntities = orderSealEntityService.findBySeal(seal.get());
        for (OrderSealEntity orderSealEntity : orderSealEntities) {
            orderSealEntity.setStatus(CommonStatusEnum.DELETED.name());
            orderSealEntity.setSealRemovalTime(LocalDateTime.now()); // Set thời gian gỡ bỏ seal
            orderSeals.add(orderSealEntity);
        }
        orderSealEntityService.saveAll(orderSeals);
        seal.get().setStatus(CommonStatusEnum.DELETED.name());

        return new GetSealFullResponse(
                sealMapper.toGetSealResponse(seal.get()),
                orderSealMapper.toGetOrderSealResponses(orderSeals)
        );
    }

    @Override
    public GetSealFullResponse getAllBySealId(UUID sealId) {
        Optional<SealEntity> seal = sealEntityService.findEntityById(sealId);
        if (seal.isEmpty()) {
            throw new NotFoundException("Không tìm thấy seal với ID: " + sealId, ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<OrderSealEntity> orderSealEntities = orderSealEntityService.findBySeal(seal.get());

        return new GetSealFullResponse(
                sealMapper.toGetSealResponse(seal.get()),
                orderSealMapper.toGetOrderSealResponses(orderSealEntities)
        );
    }

    @Override
    public GetOrderSealResponse getActiveOrderSealByVehicleAssignmentId(UUID vehicleAssignmentId) {
        Optional<VehicleAssignmentEntity> vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId);
        if (vehicleAssignment.isEmpty()) {
            throw new NotFoundException("Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId, ErrorEnum.NOT_FOUND.getErrorCode());
        }
        OrderSealEntity existingSeal = orderSealEntityService.findByVehicleAssignment(vehicleAssignment.get(), CommonStatusEnum.ACTIVE.name());
        return orderSealMapper.toGetOrderSealResponse(existingSeal);
    }

    @Override
    public List<GetOrderSealResponse> getAllOrderSealsByVehicleAssignmentId(UUID vehicleAssignmentId) {
        Optional<VehicleAssignmentEntity> vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId);
        if (vehicleAssignment.isEmpty()) {
            throw new NotFoundException("Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId,
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        // Get all order seals for this vehicle assignment, regardless of status
        List<OrderSealEntity> orderSeals = orderSealEntityService.findAllByVehicleAssignment(vehicleAssignment.get());

        // Convert to response DTOs
        return orderSealMapper.toGetOrderSealResponses(orderSeals);
    }
}
