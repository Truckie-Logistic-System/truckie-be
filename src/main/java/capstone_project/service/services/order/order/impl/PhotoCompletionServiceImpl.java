package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.enums.SealEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CreatePhotoCompletionRequest;
import capstone_project.dtos.request.order.UpdatePhotoCompletionRequest;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.entity.order.confirmation.PhotoCompletionEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.conformation.PhotoCompletionEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.order.PhotoCompletionMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.order.OrderDetailStatusService;
import capstone_project.service.services.order.order.PhotoCompletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PhotoCompletionServiceImpl implements PhotoCompletionService {
    private final PhotoCompletionEntityService photoCompletionEntityService;
    private final PhotoCompletionMapper photoCompletionMapper;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final CloudinaryService cloudinaryService;
    private final OrderEntityService orderEntityService;
    private final OrderService orderService;
    private final OrderDetailStatusService orderDetailStatusService;
    private final SealEntityService sealEntityService;

    public PhotoCompletionServiceImpl(
            PhotoCompletionEntityService photoCompletionEntityService,
            PhotoCompletionMapper photoCompletionMapper,
            VehicleAssignmentEntityService vehicleAssignmentEntityService,
            CloudinaryService cloudinaryService,
            OrderEntityService orderEntityService,
            @Lazy OrderService orderService,
            OrderDetailStatusService orderDetailStatusService,
            SealEntityService sealEntityService) {
        this.photoCompletionEntityService = photoCompletionEntityService;
        this.photoCompletionMapper = photoCompletionMapper;
        this.vehicleAssignmentEntityService = vehicleAssignmentEntityService;
        this.cloudinaryService = cloudinaryService;
        this.orderEntityService = orderEntityService;
        this.orderService = orderService;
        this.orderDetailStatusService = orderDetailStatusService;
        this.sealEntityService = sealEntityService;
    }

    @Override
    public PhotoCompletionResponse uploadAndSavePhoto(MultipartFile file,
                                                      CreatePhotoCompletionRequest request) throws IOException {
        // upload Cloudinary
        var uploadResult = cloudinaryService.uploadFile(
                file.getBytes(),
                UUID.randomUUID().toString(),
                "photo_completions"
        );
        String imageUrl = uploadResult.get("secure_url").toString();

        // load relationships
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new RuntimeException("VehicleAssignment not found"));

        // save DB
        PhotoCompletionEntity entity = PhotoCompletionEntity.builder()
                .imageUrl(imageUrl)
                .description(request.description())
                .vehicleAssignmentEntity(vehicleAssignment)
                .build();
        entity = photoCompletionEntityService.save(entity);

        return photoCompletionMapper.toPhotoCompletionResponse(entity);
    }

    @Override
    public List<PhotoCompletionResponse> uploadAndSaveMultiplePhotos(List<MultipartFile> files,
                                                     CreatePhotoCompletionRequest request) throws IOException {
        // load relationships - only do this once for all photos
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new RuntimeException("VehicleAssignment not found"));

        List<PhotoCompletionEntity> savedEntities = new ArrayList<>();

        for (MultipartFile file : files) {
            // upload each file to Cloudinary
            var uploadResult = cloudinaryService.uploadFile(
                    file.getBytes(),
                    UUID.randomUUID().toString(),
                    "photo_completions"
            );
            String imageUrl = uploadResult.get("secure_url").toString();

            // save each photo to DB
            PhotoCompletionEntity entity = PhotoCompletionEntity.builder()
                    .imageUrl(imageUrl)
                    .description(request.description())
                    .vehicleAssignmentEntity(vehicleAssignment)
                    .build();
            entity = photoCompletionEntityService.save(entity);
            savedEntities.add(entity);
        }

        // ✅ CRITICAL: Auto-update OrderDetail status to DELIVERED after photo upload
        // This also triggers Order status aggregation (multi-trip logic)
        try {
            orderDetailStatusService.updateOrderDetailStatusByAssignment(
                    request.vehicleAssignmentId(),
                    OrderDetailStatusEnum.DELIVERED
            );

        } catch (Exception e) {
            log.warn("⚠️ Failed to auto-update OrderDetail status: {}", e.getMessage());
            // Don't fail the main operation - photos were uploaded successfully
        }

        // 🔐 SEAL REMOVAL: Removed auto-removal logic
        // Driver will manually report seal removal when needed through the app
        // This prevents conflicts with return goods flow where seal removal report is required AFTER customer payment

        return photoCompletionMapper.toPhotoCompletionResponses(savedEntities);
    }

    @Override
    public PhotoCompletionResponse updatePhoto(UpdatePhotoCompletionRequest request) {
        PhotoCompletionEntity entity = photoCompletionEntityService.findEntityById(request.id())
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        entity.setDescription(request.description());
        entity = photoCompletionEntityService.save(entity);

        return photoCompletionMapper.toPhotoCompletionResponse(entity);
    }

//    @Override
//    public void deletePhoto(UUID id) {
//        PhotoCompletionEntity entity = photoCompletionEntityService.findContractRuleEntitiesById(id)
//                .orElseThrow(() -> new RuntimeException("Photo not found"));
//        entity.set
//
//        photoCompletionEntityService.de(entity);
//    }

    @Override
    public PhotoCompletionResponse getPhoto(UUID id) {
        return photoCompletionMapper.toPhotoCompletionResponse(photoCompletionEntityService.findEntityById(id).get());
    }

    @Override
    public List<PhotoCompletionResponse> getAllPhotos() {
        return photoCompletionMapper.toPhotoCompletionResponses(photoCompletionEntityService.findAll());
    }

    @Override
    public List<PhotoCompletionResponse> getByVehicleAssignmentId(UUID vehicleAssignmentId) {
        Optional<VehicleAssignmentEntity> vehicleAssignmentEntity = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId);
        if (vehicleAssignmentEntity.isEmpty()){
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "Vehicle assignment khong tim thay",ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return photoCompletionMapper.toPhotoCompletionResponses(photoCompletionEntityService.findByVehicleAssignmentEntity(vehicleAssignmentEntity.get()));
    }

}
