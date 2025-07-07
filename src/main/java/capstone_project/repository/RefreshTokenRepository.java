package capstone_project.repository;

import capstone_project.entity.RefreshTokenEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends BaseRepository<RefreshTokenEntity> {

    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);

    Optional<RefreshTokenEntity> findByToken(String token);
}
