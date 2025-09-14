package capstone_project.service.services.setting.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.setting.ContractSettingRequest;
import capstone_project.dtos.request.setting.UpdateContractSettingRequest;
import capstone_project.dtos.response.setting.ContractSettingResponse;
import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.service.mapper.setting.ContractSettingMapper;
import capstone_project.service.services.setting.ContractSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractSettingServiceImpl implements ContractSettingService {

    private final ContractSettingEntityService contractSettingEntityService;
    private final ContractSettingMapper contractSettingMapper;

    @Override
    public List<ContractSettingResponse> getAllContractSettingEntities() {
        log.info("getAllContractSettingEntities");

        List<ContractSettingEntity> contractSettingEntities = contractSettingEntityService.findAll();

        if (contractSettingEntities.isEmpty()) {
            log.info("No contract settings found");
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        return contractSettingEntities.stream()
                .map(contractSettingMapper::toContractSettingResponse)
                .toList();
    }

    @Override
    public ContractSettingResponse getContractSettingById(UUID id) {
        log.info("getContractSettingById: {}", id);

        if (id == null) {
            log.info("getContractSettingById: id is null");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        ContractSettingEntity contractSettingEntity = contractSettingEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.info("Contract setting not found with id: {}", id);
                    return new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
                });

        return contractSettingMapper.toContractSettingResponse(contractSettingEntity);
    }

    @Override
    public ContractSettingResponse createContractSetting(ContractSettingRequest contractSettingRequest) {
        log.info("createContractSetting: {}", contractSettingRequest);

        if (contractSettingRequest == null) {
            log.info("createContractSetting: contractSettingRequest is null");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        ContractSettingEntity contractSettingEntity = contractSettingMapper.mapRequestToEntity(contractSettingRequest);

        ContractSettingEntity savedEntity = contractSettingEntityService.save(contractSettingEntity);

        return contractSettingMapper.toContractSettingResponse(savedEntity);
    }

    @Override
    public ContractSettingResponse updateContractSetting(UUID id, UpdateContractSettingRequest contractSettingRequest) {
        log.info("updateContractSetting: id = {}, request = {}", id, contractSettingRequest);

        if (id == null || contractSettingRequest == null) {
            log.info("updateContractSetting: id or contractSettingRequest is null");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        ContractSettingEntity existingEntity = contractSettingEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        contractSettingMapper.toContractSettingEntity(contractSettingRequest, existingEntity);

        ContractSettingEntity updatedEntity = contractSettingEntityService.save(existingEntity);

        return contractSettingMapper.toContractSettingResponse(updatedEntity);
    }

    @Override
    public void deleteContractSetting(UUID id) {

    }
}
