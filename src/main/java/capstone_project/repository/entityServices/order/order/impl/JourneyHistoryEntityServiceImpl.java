package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.repository.repositories.order.order.JourneyHistoryRepository;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JourneyHistoryEntityServiceImpl implements JourneyHistoryEntityService {

    private final JourneyHistoryRepository journeyHistoryRepository;

    @Override
    public JourneyHistoryEntity save(JourneyHistoryEntity entity) {
        return journeyHistoryRepository.save(entity);
    }

    @Override
    public Optional<JourneyHistoryEntity> findEntityById(UUID uuid) {
        return journeyHistoryRepository.findById(uuid);
    }

    @Override
    public List<JourneyHistoryEntity> findAll() {
        return journeyHistoryRepository.findAll();
    }
}
