package capstone_project.repository.entityServices.setting.impl;

import capstone_project.entity.setting.StipulationSettingEntity;
import capstone_project.repository.entityServices.setting.StipulationSettingEntityService;
import capstone_project.repository.repositories.setting.StipulationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StipulationSettingEntityServiceImpl implements StipulationSettingEntityService {

    private final StipulationSettingRepository stipulationSettingRepository;

    @Override
    public StipulationSettingEntity save(StipulationSettingEntity entity) {
        return stipulationSettingRepository.save(entity);
    }

    @Override
    public Optional<StipulationSettingEntity> findEntityById(UUID uuid) {
        return stipulationSettingRepository.findById(uuid);
    }

    @Override
    public List<StipulationSettingEntity> findAll() {
        return stipulationSettingRepository.findAll();
    }

    @Override
    public Optional<StipulationSettingEntity> findTopByOrderByIdAsc() {
        return stipulationSettingRepository.findTopByOrderByIdAsc();
    }

    @Override
    public void deleteById(UUID id) {
        stipulationSettingRepository.deleteById(id);
    }
}
