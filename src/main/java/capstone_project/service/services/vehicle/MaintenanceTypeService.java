package capstone_project.service.services.vehicle;

import capstone_project.dtos.request.vehicle.MaintenanceTypeRequest;
import capstone_project.dtos.request.vehicle.UpdateMaintenanceTypeRequest;
import capstone_project.dtos.response.vehicle.MaintenanceTypeResponse;

import java.util.List;
import java.util.UUID;

public interface MaintenanceTypeService {
    List<MaintenanceTypeResponse> getAll();
    MaintenanceTypeResponse getById(UUID id);
    MaintenanceTypeResponse create(MaintenanceTypeRequest req);
    MaintenanceTypeResponse update(UUID id, UpdateMaintenanceTypeRequest req);
    void delete(UUID id);
}
