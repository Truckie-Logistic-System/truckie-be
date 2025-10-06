package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.BillOfLandingPreviewResponse;
import capstone_project.dtos.response.order.BillOfLandingResponse;
import capstone_project.service.services.billOfLanding.BillOfLandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("${bill-of-lading.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BillOfLadingController {

    private final BillOfLandingService billOfLadingService;

    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<BillOfLandingResponse>> getAllInformationForBillOfLanding(@PathVariable UUID contractId) {
        final var result = billOfLadingService.getBillOfLandingById(contractId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/order/{orderId}/preview")
    public ResponseEntity<ApiResponse<List<BillOfLandingPreviewResponse>>> previewWaybillsAndManifestsByOrder(@PathVariable UUID orderId) {
        List<BillOfLandingPreviewResponse> previews = billOfLadingService.getBillOfLadingAndCargoManifestsPreview(orderId);
        return ResponseEntity.ok(ApiResponse.ok(previews));
    }

    @GetMapping("/order/{orderId}/print")
    public ResponseEntity<byte[]> downloadWaybillsAndManifestsByOrder(@PathVariable UUID orderId) {
        Map<String, byte[]> files = billOfLadingService.generateBillOfLadingAndCargoManifests(orderId);

        if (files == null || files.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String fileName = entry.getKey();
                byte[] content = entry.getValue() != null ? entry.getValue() : new byte[0];

                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                zos.write(content);
                zos.closeEntry();
            }
            zos.finish();

            byte[] zipBytes = baos.toByteArray();
            String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String attachmentName = "van-don-" + orderId + "-" + dateStr + ".zip";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentName + "\"")
                    .body(zipBytes);

        } catch (IOException e) {
            throw new RuntimeException("Failed to build ZIP for order " + orderId, e);
        }
    }
}
