package capstone_project.service.entityServices.impl;

import capstone_project.entity.RefreshTokenEntity;
import capstone_project.repository.RefreshTokenRepository;
import capstone_project.service.entityServices.RefreshTokenEntityService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RefreshTokenEntityServiceImpl implements RefreshTokenEntityService {

    private RefreshTokenRepository refreshTokenRepository;

    @Override
    public List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId) {
        return refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
    }

    @Override
    public Optional<RefreshTokenEntity> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public void saveAll(List<RefreshTokenEntity> tokens) {
        refreshTokenRepository.saveAll(tokens);
    }

    @Override
    public RefreshTokenEntity save(RefreshTokenEntity entity) {
        return refreshTokenRepository.save(entity);
    }

    @Override
    public Optional<RefreshTokenEntity> findById(UUID uuid) {
        return refreshTokenRepository.findById(uuid);
    }

    @Override
    public List<RefreshTokenEntity> findAll() {
        return refreshTokenRepository.findAll();
    }
}
