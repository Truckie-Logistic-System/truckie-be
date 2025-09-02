package capstone_project.service.services.order.order.impl;

import capstone_project.dtos.request.order.CreatePhotoCompletionRequest;
import capstone_project.dtos.request.order.UpdatePhotoCompletionRequest;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.entity.device.DeviceEntity;
import capstone_project.entity.order.conformation.PhotoCompletionEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.service.entityServices.device.DeviceEntityService;
import capstone_project.service.entityServices.order.conformation.PhotoCompletionEntityService;
import capstone_project.service.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.order.PhotoCompletionMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.PhotoCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoCompletionServiceImpl implements PhotoCompletionService {
    private final PhotoCompletionEntityService photoCompletionEntityService;
    private final PhotoCompletionMapper photoCompletionMapper;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final DeviceEntityService deviceEntityService;
    private final CloudinaryService cloudinaryService;


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
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findContractRuleEntitiesById(request.vehicleAssignmentId())
                .orElseThrow(() -> new RuntimeException("VehicleAssignment not found"));

        DeviceEntity device = deviceEntityService.findContractRuleEntitiesById(request.deviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // save DB
        PhotoCompletionEntity entity = PhotoCompletionEntity.builder()
                .imageUrl(imageUrl)
                .description(request.description())
                .vehicleAssignmentEntity(vehicleAssignment)
                .deviceEntity(device)
                .build();
        entity = photoCompletionEntityService.save(entity);

        return photoCompletionMapper.toPhotoCompletionResponse(entity);
    }

    @Override
    public PhotoCompletionResponse updatePhoto(UpdatePhotoCompletionRequest request) {
        PhotoCompletionEntity entity = photoCompletionEntityService.findContractRuleEntitiesById(request.id())
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
        return photoCompletionMapper.toPhotoCompletionResponse(photoCompletionEntityService.findContractRuleEntitiesById(id).get());
    }

    @Override
    public List<PhotoCompletionResponse> getAllPhotos() {
        return photoCompletionMapper.toPhotoCompletionResponses(photoCompletionEntityService.findAll());
    }

}
