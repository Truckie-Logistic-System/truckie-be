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

    @Override
    public List<GetBasingPriceResponse> getBasingPrices() {

        List<BasingPriceEntity> pricingEntities = basingPriceEntityService.findAll();
        if (pricingEntities.isEmpty()) {
            log.warn("No basing prices found");
            throw new NotFoundException(
                    "No basing prices found",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return pricingEntities.stream()
                .map(basingPriceMapper::toGetBasingPriceResponse)
                .toList();
    }

    @Override
    public GetBasingPriceResponse getBasingPriceById(UUID id) {

        if (id == null) {
            log.error("Basing price ID is required");
            throw new BadRequestException(
                    "Basing price ID is required",
                    ErrorEnum.REQUIRED.getErrorCode()
            );
        }
        BasingPriceEntity basingPriceEntity = basingPriceEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Basing price not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return basingPriceMapper.toGetBasingPriceResponse(basingPriceEntity);
    }

    @Override
    public BasingPriceResponse createBasingPrice(BasingPriceRequest basingPriceRequest) {
        
        if (basingPriceRequest.distanceRuleId() == null || basingPriceRequest.distanceRuleId().isEmpty()) {
            log.error("Distance rule ID is required for creating a basing price");
            throw new BadRequestException(
                    "Distance rule ID is required",
                    ErrorEnum.REQUIRED.getErrorCode()
            );
        }

        UUID sizeRuleId;
        UUID distanceRuleId;
        try {
            sizeRuleId = UUID.fromString(basingPriceRequest.sizeRuleId());
            distanceRuleId = UUID.fromString(basingPriceRequest.distanceRuleId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid vehicle rule ID or distance rule ID format: {}", basingPriceRequest.sizeRuleId());
            throw new BadRequestException(
                    "Invalid vehicle rule ID or distance rule ID format",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        Optional<BasingPriceEntity> existingBasingPrice = basingPriceEntityService.findBasingPriceEntityBysizeRuleEntityIdAndDistanceRuleEntityId(
                sizeRuleId, distanceRuleId);

        if (existingBasingPrice.isPresent()) {
            log.error("Basing price already exists for vehicle rule ID: {} and distance rule ID: {}", sizeRuleId, distanceRuleId);
            throw new BadRequestException(
                    "Basing price already exists for the given vehicle rule and distance rule",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        BasingPriceEntity basingPriceEntity = basingPriceMapper.mapRequestToEntity(basingPriceRequest);

        BasingPriceEntity savedEntity = basingPriceEntityService.save(basingPriceEntity);

        return basingPriceMapper.toBasingPriceResponse(savedEntity);
    }

    @Override
    public BasingPriceResponse updateBasingPrice(UUID id, UpdateBasingPriceRequest basingPriceRequest) {

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

        return basingPriceMapper.toBasingPriceResponse(savedEntity);
    }

    @Override
    public void deleteBasingPrice(UUID id) {

    }
}
