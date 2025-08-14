package capstone_project.service.entityServices.user.impl;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.user.PenaltyHistoryRepository;
import capstone_project.service.entityServices.user.PenaltyHistoryEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PenaltyHistoryEntityServiceImpl
        implements PenaltyHistoryEntityService {

    private final PenaltyHistoryRepository repository;

    @Override public PenaltyHistoryEntity save(PenaltyHistoryEntity e){return repository.save(e);}
    @Override public Optional<PenaltyHistoryEntity> findById(UUID id){return repository.findById(id);}
    @Override public List<PenaltyHistoryEntity> findAll(){return repository.findAll();}
}