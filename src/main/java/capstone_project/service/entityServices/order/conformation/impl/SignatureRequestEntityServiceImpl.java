package capstone_project.service.entityServices.order.conformation.impl;

import capstone_project.entity.order.conformation.SignatureRequestEntity;
import capstone_project.repository.order.conformation.SignatureRequestRepository;
import capstone_project.service.entityServices.order.conformation.SignatureRequestEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignatureRequestEntityServiceImpl implements SignatureRequestEntityService {

    private final SignatureRequestRepository signatureRequestRepository;

    @Override
    public SignatureRequestEntity save(SignatureRequestEntity entity) {
        return signatureRequestRepository.save(entity);
    }

    @Override
    public Optional<SignatureRequestEntity> findContractRuleEntitiesById(UUID uuid) {
        return signatureRequestRepository.findById(uuid);
    }

    @Override
    public List<SignatureRequestEntity> findAll() {
        return signatureRequestRepository.findAll();
    }
}
