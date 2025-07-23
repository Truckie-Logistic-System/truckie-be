package capstone_project.repository.auth;

import capstone_project.entity.auth.RefreshTokenEntity;
import capstone_project.repository.common.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends BaseRepository<RefreshTokenEntity> {

    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);

    Optional<RefreshTokenEntity> findByToken(String token);
}
