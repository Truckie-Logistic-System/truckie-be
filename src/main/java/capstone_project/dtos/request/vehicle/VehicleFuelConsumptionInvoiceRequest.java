package capstone_project.dtos.request.vehicle;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record VehicleFuelConsumptionInvoiceRequest(
    @NotNull UUID id,
    @NotNull MultipartFile companyInvoiceImage
) {}
