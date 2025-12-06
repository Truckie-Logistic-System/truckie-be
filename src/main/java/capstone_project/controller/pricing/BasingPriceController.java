package capstone_project.controller.pricing;

import capstone_project.dtos.request.pricing.BasingPriceRequest;
import capstone_project.dtos.request.pricing.UpdateBasingPriceRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.pricing.BasingPriceResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceResponse;
import capstone_project.service.services.pricing.BasingPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${basing-price.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BasingPriceController {
    private final BasingPriceService basingPriceService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<GetBasingPriceResponse>>> getAllBasingRules() {
        final var result = basingPriceService.getBasingPrices();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetBasingPriceResponse>> getBasingRuleById(@PathVariable("id") final UUID id) {
        final var result = basingPriceService.getBasingPriceById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @PostMapping()
    public ResponseEntity<ApiResponse<BasingPriceResponse>> createBasingRule(@RequestBody @Valid BasingPriceRequest basingPriceRequest) {
        final var result = basingPriceService.createBasingPrice(basingPriceRequest);
        return ResponseEntity.status(201).body(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BasingPriceResponse>> updateBasingRule(@PathVariable("id") final UUID id, @RequestBody @Valid UpdateBasingPriceRequest basingPriceRequest) {
        final var result = basingPriceService.updateBasingPrice(id, basingPriceRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteBasingRule(@PathVariable("id") final UUID id) {
        basingPriceService.deleteBasingPrice(id);
        return ResponseEntity.ok(ApiResponse.ok("Basing price deleted successfully"));
    }

}
