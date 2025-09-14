package capstone_project.repository.entityServices.setting.impl;

import capstone_project.entity.setting.SystemSettingEntity;
import capstone_project.repository.entityServices.setting.SystemSettingEntityService;
import capstone_project.repository.repositories.setting.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingEntityServiceImpl implements SystemSettingEntityService {

    private final SystemSettingRepository systemSettingRepository;

    @Override
    public SystemSettingEntity save(SystemSettingEntity entity) {
        return systemSettingRepository.save(entity);
    }

    @Override
    public Optional<SystemSettingEntity> findEntityById(UUID uuid) {
        return systemSettingRepository.findById(uuid);
    }

    @Override
    public List<SystemSettingEntity> findAll() {
        return systemSettingRepository.findAll();
    }
}
