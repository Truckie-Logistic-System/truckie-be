package capstone_project.controller.controllers;

import capstone_project.controller.dtos.request.RegisterDriverRequest;
import capstone_project.controller.dtos.response.ApiResponse;
import capstone_project.controller.dtos.response.DriverResponse;
import capstone_project.service.services.RegistersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${manager.api.base-path}")
@RequiredArgsConstructor
public class AccountsManagerController {

    private final RegistersService registersService;

    @PostMapping("/register/driver")
    public ResponseEntity<ApiResponse<DriverResponse>> registerDriver(@RequestBody @Valid RegisterDriverRequest registerDriverRequest) {
        final var register = registersService.registerDriver(registerDriverRequest);
        return ResponseEntity.ok(ApiResponse.ok(register));
    }
}
