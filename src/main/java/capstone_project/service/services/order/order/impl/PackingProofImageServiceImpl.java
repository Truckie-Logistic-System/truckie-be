package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CreatePackingProofImageRequest;
import capstone_project.dtos.request.order.UpdatePackingProofImageRequest;
import capstone_project.dtos.response.order.PackingProofImageResponse;
import capstone_project.entity.order.confirmation.PackingProofImageEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.conformation.PackingProofImageEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.order.PackingProofImageMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.PackingProofImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackingProofImageServiceImpl implements PackingProofImageService {
    private final PackingProofImageEntityService packingProofImageEntityService;
    private final PackingProofImageMapper packingProofImageMapper;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final CloudinaryService cloudinaryService;

    @Override
    public PackingProofImageResponse uploadAndSaveImage(MultipartFile file,
                                                     CreatePackingProofImageRequest request) throws IOException {
        // upload to Cloudinary
        var uploadResult = cloudinaryService.uploadFile(
                file.getBytes(),
                UUID.randomUUID().toString(),
                "packing_proof_images"
        );
        String imageUrl = uploadResult.get("secure_url").toString();

        // load relationships
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new RuntimeException("VehicleAssignment not found"));

        // save to database
        PackingProofImageEntity entity = PackingProofImageEntity.builder()
                .imageUrl(imageUrl)
                .description(request.description())
                .vehicleAssignmentEntity(vehicleAssignment)
                .build();
        entity = packingProofImageEntityService.save(entity);

        return packingProofImageMapper.toPackingProofImageResponse(entity);
    }

    @Override
    public PackingProofImageResponse updateImage(UpdatePackingProofImageRequest request) {
        PackingProofImageEntity entity = packingProofImageEntityService.findEntityById(request.id())
                .orElseThrow(() -> new RuntimeException("Packing proof image not found"));

        entity.setDescription(request.description());
        entity = packingProofImageEntityService.save(entity);

        return packingProofImageMapper.toPackingProofImageResponse(entity);
    }

    @Override
    public PackingProofImageResponse getImage(UUID id) {
        return packingProofImageMapper.toPackingProofImageResponse(
                packingProofImageEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + "Packing proof image not found",
                        ErrorEnum.NOT_FOUND.getErrorCode())));
    }

    @Override
    public List<PackingProofImageResponse> getAllImages() {
        return packingProofImageMapper.toPackingProofImageResponses(packingProofImageEntityService.findAll());
    }

    @Override
    public List<PackingProofImageResponse> getByVehicleAssignmentId(UUID vehicleAssignmentId) {
        Optional<VehicleAssignmentEntity> vehicleAssignmentEntity = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId);
        if (vehicleAssignmentEntity.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "Vehicle assignment not found",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return packingProofImageMapper.toPackingProofImageResponses(
                packingProofImageEntityService.findByVehicleAssignmentEntity(vehicleAssignmentEntity.get()));
    }
}
