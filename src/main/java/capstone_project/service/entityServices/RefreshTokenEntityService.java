package capstone_project.service.entityServices;


import capstone_project.entity.RefreshTokenEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenEntityService {

    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);

    Optional<RefreshTokenEntity> findByToken(String token);

    void saveAll(List<RefreshTokenEntity> tokens);

    RefreshTokenEntity create(RefreshTokenEntity entity);
}
