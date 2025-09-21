package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.entityServices.user.PenaltyHistoryEntityService;
import capstone_project.service.mapper.user.PenaltyHistoryMapper;
import capstone_project.service.services.redis.RedisService;
import capstone_project.service.services.user.PenaltyHistoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PenaltyHistoryServiceImpl implements PenaltyHistoryService {

    private final PenaltyHistoryEntityService entityService;
    private final PenaltyHistoryMapper mapper;
    private final RedisService redis; // optional cache

    private static final String CACHE_ALL = "penalties:all";
    private static final String CACHE_BY_ID = "penalty:";
    private static final String CACHE_BY_DRIVER_ID = "penalties:driver:";

    @Override
    public List<PenaltyHistoryResponse> getAll() {
        List<PenaltyHistoryEntity> list = entityService.findAll();
        return list.stream()
                .map(mapper::toPenaltyHistoryResponse)
                .toList();
    }

    @Override
    public PenaltyHistoryResponse getById(UUID id) {
        PenaltyHistoryEntity e = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        return mapper.toPenaltyHistoryResponse(e);
    }

    @Transactional
    @Override
    public PenaltyHistoryResponse create(PenaltyHistoryRequest req) {
        PenaltyHistoryEntity saved = entityService.save(mapper.toEntity(req));
        return mapper.toPenaltyHistoryResponse(saved);
    }

    @Transactional
    @Override
    public PenaltyHistoryResponse update(UUID id, PenaltyHistoryRequest req) {
        PenaltyHistoryEntity existing = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        mapper.toEntity(req, existing);
        PenaltyHistoryEntity saved = entityService.save(existing);
        return mapper.toPenaltyHistoryResponse(saved);
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        PenaltyHistoryEntity existing = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        entityService.save(existing);               // or repository.delete(existing);
    }

    @Override
    public List<PenaltyHistoryResponse> getByDriverId(UUID driverId) {
        log.info("Getting penalty histories for driver ID: {}", driverId);
        List<PenaltyHistoryEntity> penalties = entityService.findByDriverId(driverId);
        return penalties.stream()
                .map(mapper::toPenaltyHistoryResponse)
                .toList();
    }
}
