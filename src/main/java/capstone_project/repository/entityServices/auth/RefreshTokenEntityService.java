package capstone_project.repository.entityServices.auth;


import capstone_project.entity.auth.RefreshTokenEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenEntityService extends BaseEntityService<RefreshTokenEntity, UUID> {

    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);

    Optional<RefreshTokenEntity> findByToken(String token);

    void saveAll(List<RefreshTokenEntity> tokens);
}
