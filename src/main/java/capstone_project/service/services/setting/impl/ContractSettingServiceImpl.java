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

        List<ContractSettingEntity> contractSettingEntities = contractSettingEntityService.findAll();

        if (contractSettingEntities.isEmpty()) {
            
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        return contractSettingEntities.stream()
                .map(contractSettingMapper::toContractSettingResponse)
                .toList();
    }

    @Override
    public ContractSettingResponse getContractSettingById(UUID id) {

        if (id == null) {
            
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        ContractSettingEntity contractSettingEntity = contractSettingEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    
                    return new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
                });

        return contractSettingMapper.toContractSettingResponse(contractSettingEntity);
    }

    @Override
    public ContractSettingResponse createContractSetting(ContractSettingRequest contractSettingRequest) {

        if (contractSettingRequest == null) {
            
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

        if (id == null || contractSettingRequest == null) {
            
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
