package capstone_project.service.entityServices.order.conformation.impl;

import capstone_project.entity.order.conformation.PhotoCompletionEntity;
import capstone_project.repository.order.conformation.PhotoCompletionRepository;
import capstone_project.service.entityServices.order.conformation.PhotoCompletionEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoCompletionEntityServiceImpl implements PhotoCompletionEntityService {

    private final PhotoCompletionRepository photoCompletionRepository;

    @Override
    public PhotoCompletionEntity save(PhotoCompletionEntity entity) {
        return photoCompletionRepository.save(entity);
    }

    @Override
    public Optional<PhotoCompletionEntity> findContractRuleEntitiesById(UUID uuid) {
        return photoCompletionRepository.findById(uuid);
    }

    @Override
    public List<PhotoCompletionEntity> findAll() {
        return photoCompletionRepository.findAll();
    }
}
