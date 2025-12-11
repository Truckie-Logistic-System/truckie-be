package capstone_project.entity.vehicle;

import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity quản lý lịch sử đăng kiểm và bảo trì xe
 * Gộp cả đăng kiểm và bảo trì vào cùng 1 bảng
 * serviceType lấy từ list-config.properties (vehicle.service.types)
 *
 * Các trường quan trọng:
 * - plannedDate: Ngày dự kiến thực hiện
 * - actualDate: Ngày thực tế hoàn thành
 * - nextServiceDate: Ngày bảo trì/kiểm định tiếp theo (sau khi hoàn thành)
 */
@Entity
@Table(name = "vehicle_service_record", schema = "public", catalog = "capstone-project")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleServiceRecordEntity extends BaseEntity {

    /**
     * Loại dịch vụ - lấy từ list-config.properties (vehicle.service.types)
     */
    @Size(max = 100)
    @Column(name = "service_type", length = 100)
    private String serviceType;

    /**
     * Trạng thái: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_status", length = 20)
    private VehicleServiceStatusEnum serviceStatus;

    /**
     * Ngày dự kiến thực hiện
     */
    @Column(name = "planned_date")
    private LocalDateTime plannedDate;

    /**
     * Ngày thực tế thực hiện
     */
    @Column(name = "actual_date")
    private LocalDateTime actualDate;

    /**
     * Ngày bảo trì/kiểm định tiếp theo.
     * Được set khi hoàn thành service record.
     * Dùng để theo dõi và cảnh báo khi sắp đến hạn.
     */
    @Column(name = "next_service_date")
    private LocalDateTime nextServiceDate;

    /**
     * Mô tả dịch vụ
     */
    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    /**
     * Odometer reading - map vào cột odometer_reading
     */
    @Column(name = "odometer_reading")
    private Integer odometerReading;

    /**
     * Ghi chú thêm
     */
    /**
     * Ghi chú thêm
     * 
     * Hiện tại chưa có cột tương ứng trong bảng `vehicle_maintenance`,
     * nên field này không được map xuống DB để tránh lỗi schema-validation.
     */
    @Transient
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicleEntity;

}
