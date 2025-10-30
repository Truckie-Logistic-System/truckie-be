package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CreatePackingProofImageRequest;
import capstone_project.dtos.request.order.LoadingDocumentationRequest;
import capstone_project.dtos.request.order.seal.SealRequest;
import capstone_project.dtos.response.order.LoadingDocumentationResponse;
import capstone_project.dtos.response.order.PackingProofImageResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.services.order.order.LoadingDocumentationService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.order.OrderDetailStatusService;
import capstone_project.service.services.order.order.PackingProofImageService;
import capstone_project.service.services.order.seal.SealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadingDocumentationServiceImpl implements LoadingDocumentationService {
    private final PackingProofImageService packingProofImageService;
    private final SealService sealService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderService orderService;
    private final OrderDetailStatusService orderDetailStatusService;

    @Override
    @Transactional
    public LoadingDocumentationResponse documentLoading(
            List<MultipartFile> packingProofImages,
            MultipartFile sealImage,
            LoadingDocumentationRequest request) throws IOException {

        log.info("Documenting loading for vehicle assignment ID: {}", request.vehicleAssignmentId());

        // Validate vehicle assignment exists
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle assignment not found with ID: " + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        // Auto-generate packing description
        String generatedDescription = "Packing proof for vehicle assignment " + request.vehicleAssignmentId();

        // Upload packing proof images
        List<PackingProofImageResponse> packingProofResponses = new ArrayList<>();
        for (MultipartFile file : packingProofImages) {
            CreatePackingProofImageRequest imageRequest = new CreatePackingProofImageRequest(
                    request.vehicleAssignmentId(),
                    generatedDescription // Use generated description
            );
            PackingProofImageResponse response = packingProofImageService.uploadAndSaveImage(file, imageRequest);
            packingProofResponses.add(response);
        }

        // Upload seal image and create seal with the provided code
        SealRequest sealRequest = new SealRequest(
                request.vehicleAssignmentId(),
                sealImage,
                request.sealCode()
        );
        sealService.confirmSealAttachment(sealRequest);
        GetSealResponse sealInfo = sealService.getSealByVehicleAssignmentId(request.vehicleAssignmentId());

        // Update order status to TRANSPORTING
        updateRelatedOrderStatus(vehicleAssignment);

        return new LoadingDocumentationResponse(
                request.vehicleAssignmentId(),
                packingProofResponses,
                sealInfo
        );
    }

    @Override
    public LoadingDocumentationResponse getLoadingDocumentation(UUID vehicleAssignmentId) {
        log.info("Getting loading documentation for vehicle assignment ID: {}", vehicleAssignmentId);

        // Validate vehicle assignment exists
        vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle assignment not found with ID: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        // Get packing proof images
        List<PackingProofImageResponse> packingProofImages =
                packingProofImageService.getByVehicleAssignmentId(vehicleAssignmentId);

        // Get seal information
        GetSealResponse sealResponse =
                sealService.getSealByVehicleAssignmentId(vehicleAssignmentId);

        return new LoadingDocumentationResponse(
                vehicleAssignmentId,
                packingProofImages,
                sealResponse
        );
    }

    /**
     * Updates the OrderDetail status to ON_DELIVERED once loading is documented
     * Order status will be auto-synced by OrderDetailStatusService
     */
    private void updateRelatedOrderStatus(VehicleAssignmentEntity vehicleAssignment) {
        try {
            // Update OrderDetail status instead of Order status
            orderDetailStatusService.updateOrderDetailStatusByAssignment(
                    vehicleAssignment.getId(),
                    OrderDetailStatusEnum.ON_DELIVERED
            );
            log.info("Auto-updated OrderDetail status: PICKING_UP → ON_DELIVERED for assignment: {}", 
                    vehicleAssignment.getId());
            // Order status will be auto-synced by OrderDetailStatusService
        } catch (Exception e) {
            log.error("Failed to update OrderDetail status after loading documentation: {}", e.getMessage());
            // Don't throw exception - loading documentation was successful
        }
    }
}
