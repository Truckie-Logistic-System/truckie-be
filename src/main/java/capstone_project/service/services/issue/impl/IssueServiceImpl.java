package capstone_project.service.services.issue.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.IssueEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.issue.CreateBasicIssueRequest;
import capstone_project.dtos.request.issue.UpdateBasicIssueRequest;
import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.service.entityServices.auth.UserEntityService;
import capstone_project.service.entityServices.issue.IssueEntityService;
import capstone_project.service.entityServices.issue.IssueTypeEntityService;
import capstone_project.service.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.issue.IssueMapper;
import capstone_project.service.services.issue.IssueService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueServiceImpl implements IssueService {
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final UserEntityService userEntityService;
    private final IssueEntityService issueEntityService;
    private final IssueTypeEntityService issueTypeEntityService;
    private final IssueMapper issueMapper;


    @Override
    public GetBasicIssueResponse getBasicIssue(UUID issueId) {
        IssueEntity getIssue = issueEntityService.findContractRuleEntitiesById(issueId).get();
        return issueMapper.toIssueBasicResponse(getIssue);
    }

    @Override
    public GetBasicIssueResponse getByVehicleAssignment(UUID vehicleAssignmentId) {
        IssueEntity entity = issueEntityService.findByVehicleAssignmentEntity(
                vehicleAssignmentEntityService.findContractRuleEntitiesById(vehicleAssignmentId).get()
        );

        return issueMapper.toIssueBasicResponse(entity);
    }

    @Override
    public List<GetBasicIssueResponse> getByStaffId(UUID staffId) {
        List<IssueEntity> entity = issueEntityService.findByStaff(
                userEntityService.findContractRuleEntitiesById(staffId).get()
        );
        return issueMapper.toIssueBasicResponses(entity);
    }

    @Override
    public List<GetBasicIssueResponse> getByActiveStatus() {
        List<IssueEntity> list = issueEntityService.findByStatus(IssueEnum.OPEN.name());
        if(list.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return issueMapper.toIssueBasicResponses(list);
    }

    @Override
    public List<GetBasicIssueResponse> getIssueType(UUID issueTypeId) {
        List<IssueEntity> issueType = issueEntityService.findByIssueTypeEntity(issueTypeEntityService.findContractRuleEntitiesById(issueTypeId).get());
        if(issueType.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return issueMapper.toIssueBasicResponses(issueType);
    }

    @Override
    public GetBasicIssueResponse createIssue(CreateBasicIssueRequest request) {
        // Lấy VehicleAssignment
        var vehicleAssignment = vehicleAssignmentEntityService.findContractRuleEntitiesById(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));


        // Lấy IssueType
        var issueType = issueTypeEntityService.findContractRuleEntitiesById(request.issueTypeId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueTypeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Tạo entity
        IssueEntity issue = IssueEntity.builder()
                .description(request.description())
                .locationLatitude(request.locationLatitude())
                .locationLongitude(request.locationLongitude())
                .status(IssueEnum.OPEN.name()) // mặc định OPEN khi tạo
                .reportedAt(java.time.LocalDateTime.now())
                .vehicleAssignmentEntity(vehicleAssignment)
                .staff(null)
                .issueTypeEntity(issueType)
                .build();

        // Lưu
        IssueEntity saved = issueEntityService.save(issue);

        // Convert sang response
        return issueMapper.toIssueBasicResponse(saved);
    }


    @Override
    public GetBasicIssueResponse updateIssue(UpdateBasicIssueRequest request) {
        // Tìm Issue cũ
        IssueEntity existing = issueEntityService.findContractRuleEntitiesById(request.issueId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Lấy IssueType mới
        var issueType = issueTypeEntityService.findContractRuleEntitiesById(request.issueTypeId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueTypeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Update thông tin
        existing.setDescription(request.description());
        existing.setLocationLatitude(request.locationLatitude());
        existing.setLocationLongitude(request.locationLongitude());
        existing.setIssueTypeEntity(issueType);

        // Lưu lại
        IssueEntity updated = issueEntityService.save(existing);

        // Convert sang response
        return issueMapper.toIssueBasicResponse(updated);
    }

    @Override
    public GetBasicIssueResponse updateStaffForIssue(UUID staffId, UUID issueId) {
        // Tìm Issue cũ
        IssueEntity existing = issueEntityService.findContractRuleEntitiesById(issueId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + issueId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        UserEntity staff = userEntityService.findContractRuleEntitiesById(staffId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + staffId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        existing.setStaff(staff);
        existing.setStatus(IssueEnum.IN_PROGRESS.name());

        // Lưu lại
        IssueEntity updated = issueEntityService.save(existing);

        // Convert sang response
        return issueMapper.toIssueBasicResponse(updated);
    }

}
