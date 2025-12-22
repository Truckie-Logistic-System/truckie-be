package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.user.AddressRequest;
import capstone_project.dtos.response.user.GeocodingResponse;
import capstone_project.dtos.response.user.AddressResponse;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.user.AddressEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.mapper.user.AddressMapper;
import capstone_project.service.services.user.AddressService;
import capstone_project.common.utils.AddressUtil;
import capstone_project.common.utils.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressEntityService addressEntityService;
    private final CustomerEntityService customerEntityService;
    private final AddressMapper addressMapper;
    private final VietMapGeocodingServiceImpl geocodingService;
    private final UserContextUtils userContextUtils;

    @Override
    public List<AddressResponse> getAllAddresses() {

        // Fetch from database
        List<AddressEntity> entities = addressEntityService.findAll();

        if (entities.isEmpty()) {
            log.warn("No addresses found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        // Convert to DTOs
        List<AddressResponse> addressResponses = entities.stream()
                .map(this::safeMapToResponse)
                .toList();

        return addressResponses;
    }

    @Override
    public List<AddressResponse> getAddressesByCustomerId(UUID customerId) {

        List<AddressEntity> entities = addressEntityService.getAddressesByCustomerId(customerId);

        if (entities.isEmpty()) {
            log.warn("No addresses found for customer ID: {}", customerId);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return entities.stream()
                .map(this::safeMapToResponse)
                .toList();
    }

    @Override
    public AddressResponse calculateLatLong(String address) {

        Optional<GeocodingResponse> geocodingResult = geocodingService.geocodeAddress(address);

        AddressResponse response;
        if (geocodingResult.isPresent()) {
            response = AddressUtil.buildResponseFromGeocoding(geocodingResult.get());
            
        } else {
            log.warn("Could not resolve coordinates via API, using fallback for address: {}", address);
            response = AddressUtil.buildFallbackResponse(address);
        }

        return response;
    }

    @Override
    public AddressResponse createAddress(AddressRequest request) {
        try {

            // Use request-provided lat/lng if present, otherwise calculate
            BigDecimal lat = null;
            BigDecimal lng = null;
            if (request.latitude() != null && request.longitude() != null) {
                lat = request.latitude();
                lng = request.longitude();
                
            } else {
                String fullAddress = AddressUtil.buildFullAddress(request);
                AddressResponse locationData = enhancedCalculateLatLong(fullAddress);
                lat = locationData.latitude();
                lng = locationData.longitude();
                
            }

            AddressEntity addressEntity = addressMapper.mapRequestToAddressEntity(request);
            AddressUtil.setCoordinatesOnEntity(addressEntity, lat, lng);

            LocalDateTime now = LocalDateTime.now();
            addressEntity.setCreatedAt(now);

            // Get current customer ID from security context instead of request
            UUID currentCustomerId = userContextUtils.getCurrentCustomerId();
            CustomerEntity customer = customerEntityService.findEntityById(currentCustomerId)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            addressEntity.setCustomer(customer);

            AddressEntity saved = addressEntityService.save(addressEntity);
            AddressResponse response = safeMapToResponse(saved);

            return response;

        } catch (Exception e) {
            log.error("Error creating address: {}", request, e);
            throw new RuntimeException("Failed to create address: " + e.getMessage(), e);
        }
    }

    @Override
    public AddressResponse updateAddress(UUID id, AddressRequest request) {

        // 1. Find the existing address
        AddressEntity existing = addressEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Address with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        // 2. Store old customer ID for cache invalidation
        String oldCustomerId = existing.getCustomer() != null ?
                existing.getCustomer().getId().toString() : null;

        // 3. Update the entity with new request data
        addressMapper.toAddressEntity(request, existing);

        BigDecimal lat = null;
        BigDecimal lng = null;
        if (request.latitude() != null && request.longitude() != null) {
            lat = request.latitude();
            lng = request.longitude();
            
        } else {
            // 4. Calculate new coordinates for the updated address or use provided ones
            String fullAddress = AddressUtil.buildFullAddress(request);
            AddressResponse locationData = enhancedCalculateLatLong(fullAddress);
            lat = locationData.latitude();
            lng = locationData.longitude();
            
        }

        AddressUtil.setCoordinatesOnEntity(existing, lat, lng);

        // Get current customer ID from security context
        UUID currentCustomerId = userContextUtils.getCurrentCustomerId();

        // 5. Save the updated entity
        AddressEntity updated = addressEntityService.save(existing);
        AddressResponse response = safeMapToResponse(updated);

        return response;
    }

    @Override
    public AddressResponse getAddressById(UUID id) {

        Optional<AddressEntity> addressEntity = addressEntityService.findEntityById(id);

        return addressEntity
                .map(entity -> {
                    AddressResponse response = safeMapToResponse(entity);
                    return response;
                })
                .orElseThrow(() -> {
                    log.warn("Address with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });
    }

    @Override
    public List<AddressResponse> getMyAddresses() {
        UUID customerId = userContextUtils.getCurrentCustomerId();
        List<AddressEntity> entities = addressEntityService.getAddressesByCustomerId(customerId);
        return entities.stream()
                .map(this::safeMapToResponse)
                .toList();
    }

    /**
     * Safely map AddressEntity to AddressResponse, handling null customer relationships
     */
    private AddressResponse safeMapToResponse(AddressEntity entity) {
        try {
            return addressMapper.toAddressResponse(entity);
        } catch (Exception e) {
            log.warn("Error mapping entity to response, using manual mapping for ID: {}", entity.getId(), e);

            // Manual mapping as fallback
            String customerId = null;
            if (entity.getCustomer() != null) {
                customerId = entity.getCustomer().getId().toString();
            }

            return new AddressResponse(
                    entity.getId() != null ? entity.getId().toString() : null,
                    entity.getProvince(),
                    entity.getWard(),
                    entity.getStreet(),
                    entity.getAddressType(),
                    entity.getLatitude(),
                    entity.getLongitude(),
                    customerId
            );
        }
    }

    @Override
    public List<AddressResponse> getMyDeliveryAddress() {
        UUID customerId = userContextUtils.getCurrentCustomerId();

        List<AddressEntity> entities = addressEntityService.getAddressesByCustomerId(customerId);

        List<AddressResponse> responses = entities.stream()
                .filter(e -> Boolean.FALSE.equals(e.getAddressType()))
                .map(this::safeMapToResponse)
                .toList();

        if (responses.isEmpty()) {
            log.warn("Delivery addresses (addressType=false) not found for customer: {}", customerId);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return responses;
    }

    @Override
    public AddressResponse enhancedCalculateLatLong(String address) {

        // Use VietMap geocoding service for enhanced address resolution
        Optional<GeocodingResponse> geocodingResult = geocodingService.geocodeAddress(address);
        
        if (geocodingResult.isPresent()) {
            AddressResponse response = AddressUtil.buildResponseFromGeocoding(geocodingResult.get());
            
            return response;
        } else {
            log.warn("Could not resolve coordinates via VietMap, using fallback for address: {}", address);
            return AddressUtil.buildFallbackResponse(address);
        }
    }
}
