package capstone_project.dtos.response.issue;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record GetBasicIssueResponse (
        UUID id,
        String description,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        String status,
        VehicleAssignmentEntity vehicleAssignmentEntity,
        UserEntity staff,
        IssueTypeEntity issueTypeEntity
) {
}
