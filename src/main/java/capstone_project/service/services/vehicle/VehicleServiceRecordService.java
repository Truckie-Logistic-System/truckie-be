package capstone_project.service.services.vehicle;

import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.dtos.request.vehicle.UpdateVehicleServiceRecordRequest;
import capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest;
import capstone_project.dtos.response.vehicle.PaginatedServiceRecordsResponse;
import capstone_project.dtos.response.vehicle.VehicleServiceRecordResponse;

import java.util.List;
import java.util.UUID;


public interface VehicleServiceRecordService {
    List<VehicleServiceRecordResponse> getAllRecords();
    VehicleServiceRecordResponse getRecordById(UUID id);
    VehicleServiceRecordResponse createRecord(VehicleServiceRecordRequest req);
    VehicleServiceRecordResponse updateRecord(UUID id, UpdateVehicleServiceRecordRequest req);
    
    /**
     * Lấy danh sách records phân trang, sắp xếp theo ngày tạo giảm dần
     */
    PaginatedServiceRecordsResponse getAllRecordsPaginated(int page, int size);
    
    /**
     * Lấy danh sách records theo loại dịch vụ (String từ list-config)
     */
    PaginatedServiceRecordsResponse getRecordsByType(String serviceType, int page, int size);
    
    /**
     * Lấy danh sách records theo trạng thái
     */
    PaginatedServiceRecordsResponse getRecordsByStatus(VehicleServiceStatusEnum serviceStatus, int page, int size);
    
    /**
     * Lấy danh sách records theo xe
     */
    List<VehicleServiceRecordResponse> getRecordsByVehicleId(UUID vehicleId);
    
    /**
     * Hoàn thành một record bảo trì/đăng kiểm và cập nhật thông tin xe
     */
    VehicleServiceRecordResponse completeRecord(UUID id);
    
    /**
     * Hủy một record bảo trì/đăng kiểm
     */
    VehicleServiceRecordResponse cancelRecord(UUID id);
    
    /**
     * Bắt đầu một record bảo trì/đăng kiểm và cập nhật trạng thái xe thành MAINTENANCE
     */
    VehicleServiceRecordResponse startRecord(UUID id);
    
    /**
     * Lấy danh sách các loại dịch vụ từ config
     */
    List<String> getServiceTypes();

    /**
     * Tạo tự động các lịch đăng kiểm / bảo trì cho tất cả xe trong hệ thống.
     *
     * Trả về số bản ghi đã được tạo.
     */
    int generateServiceRecordsForAllVehicles();

    /**
     * Lấy danh sách các lịch sắp đến hạn (trong vòng warningDays ngày)
     */
    List<VehicleServiceRecordResponse> getServicesDueSoon(int warningDays);

    /**
     * Lấy danh sách các lịch đã quá hạn
     */
    List<VehicleServiceRecordResponse> getOverdueServices();

    /**
     * API phục vụ mục đích test UI: tạo ngẫu nhiên một số lịch bảo trì/đăng kiểm
     * với các mốc thời gian quá hạn / gấp (≤7 ngày) / cảnh báo (8-30 ngày) cho một
     * vài xe bất kỳ. Đồng thời cập nhật các trường hạn trong VehicleEntity để
     * banner cảnh báo trên FE có dữ liệu thật để hiển thị.
     *
     * @return số bản ghi dịch vụ đã được tạo
     */
    int generateDemoAlertDataForBanner();
}
