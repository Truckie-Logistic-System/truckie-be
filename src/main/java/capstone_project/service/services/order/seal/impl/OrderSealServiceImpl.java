package capstone_project.service.services.order.seal.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderSealEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.seal.OrderSealRequest;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.entity.order.order.OrderSealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.order.OrderSealEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.order.OrderSealMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.seal.OrderSealService;
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
    private final OrderSealMapper orderSealMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public GetOrderSealResponse confirmSealAttachment(OrderSealRequest orderSealRequest) {
        // Lấy vehicle assignment
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(orderSealRequest.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + orderSealRequest.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        // Kiểm tra nếu đã có seal đang được sử dụng
        OrderSealEntity existingInUseSeal = orderSealEntityService.findByVehicleAssignment(vehicleAssignment, OrderSealEnum.IN_USE.name());
        if (existingInUseSeal != null) {
            throw new BadRequestException(
                    "Seal này vẫn đang được sử dụng không thể tạo Seal mới, xóa seal cũ trước khi tạo seal mới cho chuyến đi",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Tìm seal có trạng thái ACTIVE cho vehicle assignment này
        OrderSealEntity existingActiveSeal = orderSealEntityService.findByVehicleAssignment(vehicleAssignment, OrderSealEnum.ACTIVE.name());
        if (existingActiveSeal == null) {
            throw new NotFoundException(
                    "Không tìm thấy seal ACTIVE cho vehicle assignment với ID: " + orderSealRequest.vehicleAssignmentId(),
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        // Kiểm tra seal code từ request
        if (orderSealRequest.sealCode() == null || orderSealRequest.sealCode().isEmpty()) {
            throw new BadRequestException(
                    "Mã seal không được để trống",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Kiểm tra xem seal code từ request có khớp với seal code hiện tại không
        if (!existingActiveSeal.getSealCode().equals(orderSealRequest.sealCode())) {
            throw new BadRequestException(
                    "Mã seal không khớp với seal đã được gán cho vehicle assignment này",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

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

        // Cập nhật OrderSealEntity hiện có
        existingActiveSeal.setStatus(OrderSealEnum.IN_USE.name());
        existingActiveSeal.setSealDate(LocalDateTime.now());
        existingActiveSeal.setSealAttachedImage(imageUrl);

        OrderSealEntity savedSeal = orderSealEntityService.save(existingActiveSeal);
        log.info("Đã cập nhật trạng thái seal ID {} từ ACTIVE sang IN_USE", savedSeal.getId());

        // Return OrderSealResponse directly
        return orderSealMapper.toGetOrderSealResponse(savedSeal);
    }

    @Override
    public GetOrderSealResponse removeSealBySealId(UUID sealId) {
        List<OrderSealEntity> orderSeals = new ArrayList<>();
        OrderSealEntity seal = orderSealEntityService.findEntityById(sealId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy seal với ID: " + sealId, ErrorEnum.NOT_FOUND.getErrorCode()));

        seal.setStatus(OrderSealEnum.REMOVED.name());
        seal.setSealRemovalTime(LocalDateTime.now()); // Set thời gian gỡ bỏ seal
        orderSeals.add(seal);
        orderSealEntityService.saveAll(orderSeals);

        return orderSealMapper.toGetOrderSealResponse(seal);
    }

    @Override
    public GetOrderSealResponse getAllBySealId(UUID sealId) {
        OrderSealEntity seal = orderSealEntityService.findEntityById(sealId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy seal với ID: " + sealId, ErrorEnum.NOT_FOUND.getErrorCode()));

        return orderSealMapper.toGetOrderSealResponse(seal);
    }

    @Override
    public GetOrderSealResponse getActiveOrderSealByVehicleAssignmentId(UUID vehicleAssignmentId) {
        Optional<VehicleAssignmentEntity> vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId);
        if (vehicleAssignment.isEmpty()) {
            throw new NotFoundException("Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId, ErrorEnum.NOT_FOUND.getErrorCode());
        }
        OrderSealEntity existingSeal = orderSealEntityService.findByVehicleAssignment(vehicleAssignment.get(), OrderSealEnum.ACTIVE.name());
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

    @Override
    @Transactional
    public int updateOrderSealsToUsed(VehicleAssignmentEntity vehicleAssignment) {
        if (vehicleAssignment == null) {
            log.warn("Không thể cập nhật trạng thái seal, vehicleAssignment là null");
            return 0;
        }

        // Tìm seal đang IN_USE của vehicleAssignment này
        // Chỉ lấy một seal đang IN_USE vì chỉ có 1 seal được status IN_USE tại một thời điểm
        OrderSealEntity inUseSeal = orderSealEntityService.findByVehicleAssignment(
                vehicleAssignment, OrderSealEnum.IN_USE.name());

        if (inUseSeal == null) {
            log.info("Không tìm thấy seal nào đang IN_USE cho vehicleAssignment: {}", vehicleAssignment.getId());
            return 0;
        }

        // Cập nhật seal sang trạng thái USED
        inUseSeal.setStatus(OrderSealEnum.USED.name());
        orderSealEntityService.save(inUseSeal);

        log.info("Đã cập nhật seal có ID {} sang trạng thái USED cho vehicleAssignment: {}",
                inUseSeal.getId(), vehicleAssignment.getId());

        return 1;
    }
}
