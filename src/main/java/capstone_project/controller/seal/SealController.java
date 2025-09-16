package capstone_project.controller.seal;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.service.services.order.seal.SealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${seal.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class SealController {
    private final SealService sealService;

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<GetSealResponse>>> getAll() {
        final var result = sealService.getAllSeals();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
