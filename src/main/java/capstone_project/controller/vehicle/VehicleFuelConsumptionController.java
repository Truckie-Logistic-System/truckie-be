package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionCreateRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionEndReadingRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionInvoiceRequest;
import capstone_project.dtos.response.vehicle.VehicleFuelConsumptionResponse;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.services.vehicle.VehicleFuelConsumptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-fuel-consumption.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('DRIVER', 'ADMIN')")
@Slf4j
public class VehicleFuelConsumptionController {

    private final VehicleFuelConsumptionService vehicleFuelConsumptionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text != null && !text.isEmpty()) {
                    setValue(new BigDecimal(text));
                } else {
                    setValue(null);
                }
            }
        });
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehicleFuelConsumptionResponse>> createVehicleFuelConsumption(
            @ModelAttribute VehicleFuelConsumptionCreateRequest request) {
        
        final var result = vehicleFuelConsumptionService.createVehicleFuelConsumption(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", 200, result));
    }

    @PutMapping(value = "/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehicleFuelConsumptionResponse>> updateInvoiceImage(
            @ModelAttribute VehicleFuelConsumptionInvoiceRequest request) {
        
        final var result = vehicleFuelConsumptionService.updateInvoiceImage(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", 200, result));
    }

    @PutMapping(value = "/final-reading", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehicleFuelConsumptionResponse>> updateFinalReading(
            @RequestParam UUID id,
            @RequestParam BigDecimal odometerReadingAtEnd,
            @RequestParam MultipartFile odometerAtEndImage) {

        final var request = new VehicleFuelConsumptionEndReadingRequest(id, odometerReadingAtEnd, odometerAtEndImage);
        final var result = vehicleFuelConsumptionService.updateFinalReading(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", 200, result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleFuelConsumptionResponse>> getVehicleFuelConsumptionById(
            @PathVariable UUID id) {
        
        final var result = vehicleFuelConsumptionService.getVehicleFuelConsumptionById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", 200, result));
    }

    @GetMapping("/vehicle-assignment/{vehicleAssignmentId}")
    public ResponseEntity<ApiResponse<VehicleFuelConsumptionResponse>> getVehicleFuelConsumptionByVehicleAssignmentId(
            @PathVariable UUID vehicleAssignmentId) {
        
        final var result = vehicleFuelConsumptionService.getVehicleFuelConsumptionByVehicleAssignmentId(vehicleAssignmentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", 200, result));
    }
}
