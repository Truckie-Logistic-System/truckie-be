package capstone_project.repository.entityServices.setting.impl;

import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.repository.repositories.setting.ContractSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractSettingEntityServiceImpl implements ContractSettingEntityService {

    private final ContractSettingRepository contractSettingRepository;

    @Override
    public ContractSettingEntity save(ContractSettingEntity entity) {
        return contractSettingRepository.save(entity);
    }

    @Override
    public Optional<ContractSettingEntity> findEntityById(UUID uuid) {
        return contractSettingRepository.findById(uuid);
    }

    @Override
    public List<ContractSettingEntity> findAll() {
        return contractSettingRepository.findAll();
    }
}
