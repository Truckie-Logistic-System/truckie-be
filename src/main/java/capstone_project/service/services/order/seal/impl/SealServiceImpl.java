package capstone_project.service.services.order.seal.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.SealEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.seal.SealRequest;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.order.SealMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
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
public class SealServiceImpl implements SealService {
    private final SealEntityService sealEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final SealMapper sealMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public GetSealResponse confirmSealAttachment(SealRequest sealRequest) {
        // Lấy vehicle assignment
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(sealRequest.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + sealRequest.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        // Kiểm tra nếu đã có seal đang được sử dụng
        SealEntity existingInUseSeal = sealEntityService.findByVehicleAssignment(vehicleAssignment, SealEnum.IN_USE.name());
        if (existingInUseSeal != null) {
            throw new BadRequestException(
                    "Seal này vẫn đang được sử dụng không thể tạo Seal mới, xóa seal cũ trước khi tạo seal mới cho chuyến đi",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Tìm seal có trạng thái ACTIVE cho vehicle assignment này
        SealEntity existingActiveSeal = sealEntityService.findByVehicleAssignment(vehicleAssignment, SealEnum.ACTIVE.name());
        if (existingActiveSeal == null) {
            throw new NotFoundException(
                    "Không tìm thấy seal ACTIVE cho vehicle assignment với ID: " + sealRequest.vehicleAssignmentId(),
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        // Kiểm tra seal code từ request
        if (sealRequest.sealCode() == null || sealRequest.sealCode().isEmpty()) {
            throw new BadRequestException(
                    "Mã seal không được để trống",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Kiểm tra xem seal code từ request có khớp với seal code hiện tại không
        if (!existingActiveSeal.getSealCode().equals(sealRequest.sealCode())) {
            throw new BadRequestException(
                    "Mã seal không khớp với seal đã được gán cho vehicle assignment này",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Upload hình ảnh và lấy URL
        String imageUrl = null;
        try {
            MultipartFile sealImage = sealRequest.sealImage();
            String fileName = "seal_" + UUID.randomUUID();
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                    sealImage.getBytes(),
                    fileName,
                    "seals" // folder name on Cloudinary
            );

            // Lấy URL của ảnh đã upload
            imageUrl = (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Lỗi khi upload ảnh seal: {}", e.getMessage(), e);
            throw new BadRequestException(
                    "Không thể upload ảnh seal: " + e.getMessage(),
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        // Cập nhật SealEntity hiện có
        existingActiveSeal.setStatus(SealEnum.IN_USE.name());
        existingActiveSeal.setSealDate(LocalDateTime.now());
        existingActiveSeal.setSealAttachedImage(imageUrl);

        SealEntity savedSeal = sealEntityService.save(existingActiveSeal);
        log.info("Đã cập nhật trạng thái seal ID {} từ ACTIVE sang IN_USE", savedSeal.getId());

        // Return SealResponse directly
        return sealMapper.toGetSealResponse(savedSeal);
    }

    @Override
    public GetSealResponse removeSealBySealId(UUID sealId) {
        List<SealEntity> sealEntities = new ArrayList<>();
        SealEntity seal = sealEntityService.findEntityById(sealId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy seal với ID: " + sealId, ErrorEnum.NOT_FOUND.getErrorCode()));

        seal.setStatus(SealEnum.REMOVED.name());
        seal.setSealRemovalTime(LocalDateTime.now()); // Set thời gian gỡ bỏ seal
        sealEntities.add(seal);
        sealEntityService.saveAll(sealEntities);

        return sealMapper.toGetSealResponse(seal);
    }

    @Override
    public GetSealResponse getAllBySealId(UUID sealId) {
        SealEntity seal = sealEntityService.findEntityById(sealId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy seal với ID: " + sealId, ErrorEnum.NOT_FOUND.getErrorCode()));

        return sealMapper.toGetSealResponse(seal);
    }

    @Override
    public GetSealResponse getSealByVehicleAssignmentId(UUID vehicleAssignmentId) {
        Optional<VehicleAssignmentEntity> vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId);
        if (vehicleAssignment.isEmpty()) {
            throw new NotFoundException("Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId, ErrorEnum.NOT_FOUND.getErrorCode());
        }
        SealEntity existingSeal = sealEntityService.findByVehicleAssignment(vehicleAssignment.get(), SealEnum.ACTIVE.name());
        return sealMapper.toGetSealResponse(existingSeal);
    }

    @Override
    public List<GetSealResponse> getAllSealsByVehicleAssignmentId(UUID vehicleAssignmentId) {
        Optional<VehicleAssignmentEntity> vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId);
        if (vehicleAssignment.isEmpty()) {
            throw new NotFoundException("Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId,
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        // Get all order seals for this vehicle assignment, regardless of status
        List<SealEntity> seals = sealEntityService.findAllByVehicleAssignment(vehicleAssignment.get());

        // Convert to response DTOs
        return sealMapper.toGetSealResponses(seals);
    }

    @Override
    @Transactional
    public int updateSealsToUsed(VehicleAssignmentEntity vehicleAssignment) {
        if (vehicleAssignment == null) {
            log.warn("Không thể cập nhật trạng thái seal, vehicleAssignment là null");
            return 0;
        }

        // Tìm seal đang IN_USE của vehicleAssignment này
        // Chỉ lấy một seal đang IN_USE vì chỉ có 1 seal được status IN_USE tại một thời điểm
        SealEntity inUseSeal = sealEntityService.findByVehicleAssignment(
                vehicleAssignment, SealEnum.IN_USE.name());

        if (inUseSeal == null) {
            log.info("Không tìm thấy seal nào đang IN_USE cho vehicleAssignment: {}", vehicleAssignment.getId());
            return 0;
        }

        // Cập nhật seal sang trạng thái USED
        inUseSeal.setStatus(SealEnum.USED.name());
        sealEntityService.save(inUseSeal);

        log.info("Đã cập nhật seal có ID {} sang trạng thái USED cho vehicleAssignment: {}",
                inUseSeal.getId(), vehicleAssignment.getId());

        return 1;
    }
}
