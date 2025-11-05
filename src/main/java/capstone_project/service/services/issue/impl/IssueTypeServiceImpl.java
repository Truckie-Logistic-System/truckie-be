package capstone_project.service.services.issue.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.issue.CreateIssueTypeRequest;
import capstone_project.dtos.request.issue.UpdateIssueTypeRequest;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.repository.entityServices.issue.IssueTypeEntityService;
import capstone_project.service.mapper.issue.IssueMapper;
import capstone_project.service.services.issue.IssueTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueTypeServiceImpl implements IssueTypeService {
    private final IssueTypeEntityService issueTypeEntityService;
    private final IssueMapper issueMapper;


    @Override
    public List<GetIssueTypeResponse> getAllIssueType() {
        log.info("Get all issue type");
        List<IssueTypeEntity> issueTypeEntities = issueTypeEntityService.findAll();
        if (issueTypeEntities.isEmpty()) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " getAllIssueType",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return issueMapper.toIssueTypeResponses(issueTypeEntities);
    }

    @Override
    public GetIssueTypeResponse getIssueTypeById(UUID id) {
        log.info("getIssueTypeById");
        Optional<IssueTypeEntity> issueTypeEntitie = issueTypeEntityService.findEntityById(id);
        if (issueTypeEntitie.isEmpty()) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " issueTypeEntities",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return issueMapper.toIssueTypeResponse(issueTypeEntitie.get());
    }

    @Override
    public List<GetIssueTypeResponse> getIssueTypeContainNameResponseList(String name) {
        log.info("getIssueTypeContainNameResponseList");
        if(name == null || name.isEmpty()) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " name",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        List<IssueTypeEntity> issueTypeEntities = issueTypeEntityService.findByIssueTypeNameContaining(name);
        if (issueTypeEntities.isEmpty()) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " getAllIssueType",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return issueMapper.toIssueTypeResponses(issueTypeEntities);
    }

    @Override
    public GetIssueTypeResponse createIssueType(CreateIssueTypeRequest createIssueTypeRequest) {
        if(createIssueTypeRequest == null) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " createIssueTypeRequest ",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        if(issueTypeEntityService.findByIssueTypeName(createIssueTypeRequest.name()) != null){
            throw new BadRequestException(
                    ErrorEnum.INVALID_REQUEST.getMessage() + " Name of IssueType cannot duplicate ",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        try {
            IssueTypeEntity issueTypeEntity = IssueTypeEntity.builder()
                    .issueTypeName(createIssueTypeRequest.name())
                    .description(createIssueTypeRequest.description())
                    .issueCategory(createIssueTypeRequest.issueCategory())
                    .isActive(Boolean.TRUE)
                    .build();

            return issueMapper.toIssueTypeResponse(issueTypeEntityService.save(issueTypeEntity));
        }catch (IllegalArgumentException e){
            log.error("[createIssueType] Invalid createIssueTypeRequest format");
            throw e;
        }
    }

    @Override
    public GetIssueTypeResponse updateIssueType(UpdateIssueTypeRequest updateIssueTypeRequest) {
        if (updateIssueTypeRequest == null) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " updateIssueTypeRequest ",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        // Lấy entity hiện tại từ DB
        IssueTypeEntity current = issueTypeEntityService.findEntityById(UUID.fromString(updateIssueTypeRequest.id()))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " IssueTypeEntity with ID: " + updateIssueTypeRequest.id(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Kiểm tra duplicate name nhưng loại bỏ chính record hiện tại
        IssueTypeEntity existed = issueTypeEntityService.findByIssueTypeName(updateIssueTypeRequest.name());
        if (existed != null && !existed.getId().equals(current.getId())) {
            throw new BadRequestException(
                    ErrorEnum.INVALID_REQUEST.getMessage() + " Name of IssueType cannot duplicate ",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        try {
            current.setIssueTypeName(updateIssueTypeRequest.name());
            current.setDescription(updateIssueTypeRequest.description());
            current.setIssueCategory(updateIssueTypeRequest.issueCategory());
            current.setIsActive(updateIssueTypeRequest.status());

            return issueMapper.toIssueTypeResponse(issueTypeEntityService.save(current));
        } catch (IllegalArgumentException e) {
            log.error("[updateIssueType] Invalid updateIssueTypeRequest format");
            throw e;
        }
    }

}
