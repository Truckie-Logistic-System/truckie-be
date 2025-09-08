package capstone_project.service.services.pricing.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.BasingPriceRequest;
import capstone_project.dtos.request.pricing.UpdateBasingPriceRequest;
import capstone_project.dtos.response.pricing.BasingPriceResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceResponse;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.service.mapper.order.BasingPriceMapper;
import capstone_project.service.services.pricing.BasingPriceService;
import capstone_project.service.services.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class BasingPriceServiceImpl implements BasingPriceService {

    private final BasingPriceEntityService basingPriceEntityService;
    private final BasingPriceMapper basingPriceMapper;
    private final RedisService redisService;

    private static final String BASING_PRICE_ALL_CACHE_KEY = "basing-prices:all";
    private static final String BASING_PRICE_BY_ID_CACHE_KEY_PREFIX = "basing-price:";

    @Override
    public List<GetBasingPriceResponse> getBasingPrices() {
        log.info("Fetching all basing prices");

        List<BasingPriceEntity> basingPrices = redisService.getList(BASING_PRICE_ALL_CACHE_KEY, BasingPriceEntity.class);
        if (basingPrices != null && !basingPrices.isEmpty()) {
            log.info("Returning {} basing prices from cache", basingPrices.size());
            return basingPrices.stream()
                    .map(basingPriceMapper::toGetBasingPriceResponse)
                    .toList();
        }


        List<BasingPriceEntity> pricingEntities = basingPriceEntityService.findAll();
        if (pricingEntities.isEmpty()) {
            log.warn("No basing prices found");
            throw new NotFoundException(
                    "No basing prices found",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        redisService.save(BASING_PRICE_ALL_CACHE_KEY, pricingEntities);

        return pricingEntities.stream()
                .map(basingPriceMapper::toGetBasingPriceResponse)
                .toList();
    }

    @Override
    public GetBasingPriceResponse getBasingPriceById(UUID id) {
        log.info("Fetching a basing price by id {}", id);

        if (id == null) {
            log.error("Basing price ID is required");
            throw new BadRequestException(
                    "Basing price ID is required",
                    ErrorEnum.REQUIRED.getErrorCode()
            );
        }
        String cacheKey = BASING_PRICE_BY_ID_CACHE_KEY_PREFIX + id;
        BasingPriceEntity cachedEntity = redisService.get(cacheKey, BasingPriceEntity.class);

        if (cachedEntity != null) {
            log.info("Returning cached basing price for ID: {}", id);
            return basingPriceMapper.toGetBasingPriceResponse(cachedEntity);
        } else {
            log.info("No cached basing price found for ID: {}", id);

            BasingPriceEntity basingPriceEntity = basingPriceEntityService.findEntityById(id)
                    .orElseThrow(() -> new NotFoundException(
                            "Basing price not found",
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            redisService.save(cacheKey, basingPriceEntity);

            return basingPriceMapper.toGetBasingPriceResponse(basingPriceEntity);
        }
    }

    @Override
    public BasingPriceResponse createBasingPrice(BasingPriceRequest basingPriceRequest) {
        log.info("Creating a new basing price");
        if (basingPriceRequest.distanceRuleId() == null || basingPriceRequest.distanceRuleId().isEmpty()) {
            log.error("Distance rule ID is required for creating a basing price");
            throw new BadRequestException(
                    "Distance rule ID is required",
                    ErrorEnum.REQUIRED.getErrorCode()
            );
        }

        UUID vehicleRuleId;
        UUID distanceRuleId;
        try {
            vehicleRuleId = UUID.fromString(basingPriceRequest.vehicleRuleId());
            distanceRuleId = UUID.fromString(basingPriceRequest.distanceRuleId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid vehicle rule ID or distance rule ID format: {}", basingPriceRequest.vehicleRuleId());
            throw new BadRequestException(
                    "Invalid vehicle rule ID or distance rule ID format",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        Optional<BasingPriceEntity> existingBasingPrice = basingPriceEntityService.findBasingPriceEntityByVehicleRuleEntityIdAndDistanceRuleEntityId(
                vehicleRuleId, distanceRuleId);

        if (existingBasingPrice.isPresent()) {
            log.error("Basing price already exists for vehicle rule ID: {} and distance rule ID: {}", vehicleRuleId, distanceRuleId);
            throw new BadRequestException(
                    "Basing price already exists for the given vehicle rule and distance rule",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        BasingPriceEntity basingPriceEntity = basingPriceMapper.mapRequestToEntity(basingPriceRequest);

        BasingPriceEntity savedEntity = basingPriceEntityService.save(basingPriceEntity);

        redisService.delete(BASING_PRICE_ALL_CACHE_KEY);

        log.info("Created a new basing price with ID: {}", savedEntity.getId());
        return basingPriceMapper.toBasingPriceResponse(savedEntity);
    }

    @Override
    public BasingPriceResponse updateBasingPrice(UUID id, UpdateBasingPriceRequest basingPriceRequest) {
        log.info("Updating basing price with ID: {}", id);

        if (id == null) {
            log.error("Basing price ID is required for updating a basing price");
            throw new BadRequestException(
                    "Basing price ID is required",
                    ErrorEnum.REQUIRED.getErrorCode()
            );
        }

        BasingPriceEntity existingEntity = basingPriceEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Basing price not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        basingPriceMapper.toBasingPriceEntity(basingPriceRequest, existingEntity);

        BasingPriceEntity savedEntity = basingPriceEntityService.save(existingEntity);

        redisService.delete(BASING_PRICE_ALL_CACHE_KEY);
        redisService.delete(BASING_PRICE_BY_ID_CACHE_KEY_PREFIX + id);

        log.info("Updated basing price with ID: {}", savedEntity.getId());
        return basingPriceMapper.toBasingPriceResponse(savedEntity);
    }

    @Override
    public void deleteBasingPrice(UUID id) {

    }
}
