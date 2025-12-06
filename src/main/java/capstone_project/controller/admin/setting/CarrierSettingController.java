package capstone_project.controller.admin.setting;

import capstone_project.dtos.request.setting.CarrierSettingRequest;
import capstone_project.dtos.response.setting.CarrierSettingResponse;
import capstone_project.service.services.setting.CarrierSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${carrier-setting.api.base-path}")
@RequiredArgsConstructor
@Validated
public class CarrierSettingController {

    private final CarrierSettingService service;

    @GetMapping
    public ResponseEntity<List<CarrierSettingResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarrierSettingResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<CarrierSettingResponse> create(@RequestBody @Validated CarrierSettingRequest request) {
        CarrierSettingResponse created = service.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarrierSettingResponse> update(@PathVariable UUID id,
                                                         @RequestBody @Validated CarrierSettingRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
