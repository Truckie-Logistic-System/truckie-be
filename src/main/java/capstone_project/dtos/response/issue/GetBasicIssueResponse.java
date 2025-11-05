package capstone_project.dtos.response.issue;

import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetBasicIssueResponse (
        UUID id,
        String description,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        String status,
        IssueCategoryEnum issueCategory, // GENERAL, SEAL_REPLACEMENT, ACCIDENT, PENALTY, etc.
        LocalDateTime reportedAt,
        LocalDateTime resolvedAt,
        VehicleAssignmentResponse vehicleAssignmentEntity,
        UserEntity staff,
        GetIssueTypeResponse issueTypeEntity,
        
        // Seal replacement specific fields (nullable for non-seal issues)
        GetSealResponse oldSeal,
        GetSealResponse newSeal,
        String sealRemovalImage,
        String newSealAttachedImage,
        LocalDateTime newSealConfirmedAt
) {
}
