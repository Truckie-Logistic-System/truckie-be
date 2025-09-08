
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
import capstone_project.service.services.service.RedisService;
import capstone_project.service.services.user.AddressService;
import capstone_project.common.utils.AddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressEntityService addressEntityService;
    private final CustomerEntityService customerEntityService;
    private final AddressMapper addressMapper;
    private final VietMapGeocodingServiceImpl geocodingService;
    private final RedisService redisService;

    private static final String ALL_ADDRESSES_CACHE_KEY = "addresses:all";
    private static final String ADDRESS_CACHE_KEY_PREFIX = "address:";
    private static final String GEOCODING_CACHE_KEY_PREFIX = "geocoding:";
    private static final String CUSTOMER_ADDRESSES_CACHE_KEY_PREFIX = "customer_addresses:";

    @Override
    public List<AddressResponse> getAllAddresses() {
        log.info("Fetching all addresses");

        // Check cache first for AddressResponse DTOs
        try {
            List<AddressResponse> cachedAddresses = redisService.getList(ALL_ADDRESSES_CACHE_KEY, AddressResponse.class);
            if (cachedAddresses != null && !cachedAddresses.isEmpty()) {
                log.info("Retrieved {} addresses from cache", cachedAddresses.size());
                return cachedAddresses;
            }
        } catch (Exception e) {
            log.warn("Error retrieving addresses from cache, falling back to database", e);
        }

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

        // Cache the DTOs (not entities)
        try {
            redisService.save(ALL_ADDRESSES_CACHE_KEY, addressResponses);
            redisService.expire(ALL_ADDRESSES_CACHE_KEY, 1, TimeUnit.HOURS);
            log.info("Cached {} address responses", addressResponses.size());
        } catch (Exception e) {
            log.warn("Error caching addresses, continuing without cache", e);
        }

        return addressResponses;
    }

    @Override
    public List<AddressResponse> getAddressesByCustomerId(UUID customerId) {
        log.info("Fetching addresses for customer ID: {}", customerId);

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
        log.info("Calculating lat/long for address: {}", address);

        // Check cache for geocoding result
        String geocodingCacheKey = GEOCODING_CACHE_KEY_PREFIX + address.hashCode();
        try {
            AddressResponse cachedResult = redisService.get(geocodingCacheKey, AddressResponse.class);
            if (cachedResult != null) {
                log.info("Retrieved geocoding result from cache for address: {}", address);
                return cachedResult;
            }
        } catch (Exception e) {
            log.warn("Error retrieving geocoding from cache", e);
        }

        Optional<GeocodingResponse> geocodingResult = geocodingService.geocodeAddress(address);

        AddressResponse response;
        if (geocodingResult.isPresent()) {
            response = AddressUtil.buildResponseFromGeocoding(geocodingResult.get());
            log.info("Successfully resolved coordinates - Lat: {}, Long: {}",
                    response.latitude(), response.longitude());
        } else {
            log.warn("Could not resolve coordinates via API, using fallback for address: {}", address);
            response = AddressUtil.buildFallbackResponse(address);
        }

        // Cache the geocoding result
        try {
            redisService.save(geocodingCacheKey, response);
            redisService.expire(geocodingCacheKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Error caching geocoding result", e);
        }

        return response;
    }

    @Override
    public AddressResponse createAddress(AddressRequest request) {
        try {
            String fullAddress = AddressUtil.buildFullAddress(request);
            AddressResponse locationData = calculateLatLong(fullAddress);

            AddressEntity addressEntity = addressMapper.mapRequestToAddressEntity(request);
            AddressUtil.setCoordinatesOnEntity(addressEntity, locationData.latitude(), locationData.longitude());

            LocalDateTime now = LocalDateTime.now();
            addressEntity.setCreatedAt(now);

            CustomerEntity customer = customerEntityService.findEntityById(UUID.fromString(request.customerId()))
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            addressEntity.setCustomer(customer);

            AddressEntity saved = addressEntityService.save(addressEntity);
            AddressResponse response = safeMapToResponse(saved);

            // Invalidate caches
            invalidateAddressCaches();
            invalidateCustomerAddressesCache(request.customerId());

            // Cache the new address
            try {
                String addressCacheKey = ADDRESS_CACHE_KEY_PREFIX + saved.getId();
                redisService.save(addressCacheKey, response);
                redisService.expire(addressCacheKey, 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Error caching new address", e);
            }

            return response;

        } catch (Exception e) {
            log.error("Error creating address: {}", request, e);
            throw new RuntimeException("Failed to create address: " + e.getMessage(), e);
        }
    }

    @Override
    public AddressResponse updateAddress(UUID id, AddressRequest request) {
        log.info("Updating address with ID: {}", id);

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

        // 4. Calculate new coordinates for the updated address
        String fullAddress = AddressUtil.buildFullAddress(request);
        AddressResponse locationData = calculateLatLong(fullAddress);

        AddressEntity addressEntity = addressMapper.mapRequestToAddressEntity(request);
        AddressUtil.setCoordinatesOnEntity(addressEntity, locationData.latitude(), locationData.longitude());

        // 5. Save the updated entity
        AddressEntity updated = addressEntityService.save(existing);
        AddressResponse response = safeMapToResponse(updated);

        // 6. Invalidate caches
        invalidateAddressCaches();
        invalidateCustomerAddressesCache(oldCustomerId);
        invalidateCustomerAddressesCache(request.customerId());

        // 7. Update cache with new data
        try {
            String addressCacheKey = ADDRESS_CACHE_KEY_PREFIX + id;
            redisService.save(addressCacheKey, response);
            redisService.expire(addressCacheKey, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Error updating address cache", e);
        }

        return response;
    }

    @Override
    public AddressResponse getAddressById(UUID id) {
        log.info("Fetching address with ID: {}", id);

        // Check cache first
        String cacheKey = ADDRESS_CACHE_KEY_PREFIX + id;
        try {
            AddressResponse cachedAddress = redisService.get(cacheKey, AddressResponse.class);
            if (cachedAddress != null) {
                log.info("Retrieved address from cache for ID: {}", id);
                return cachedAddress;
            }
        } catch (Exception e) {
            log.warn("Error retrieving address from cache for ID: {}", id, e);
        }

        Optional<AddressEntity> addressEntity = addressEntityService.findEntityById(id);

        return addressEntity
                .map(entity -> {
                    AddressResponse response = safeMapToResponse(entity);
                    // Cache the result
                    try {
                        redisService.save(cacheKey, response);
                        redisService.expire(cacheKey, 30, TimeUnit.MINUTES);
                        log.info("Cached address for ID: {}", id);
                    } catch (Exception e) {
                        log.warn("Error caching address for ID: {}", id, e);
                    }
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

    private void invalidateAddressCaches() {
        try {
            redisService.delete(ALL_ADDRESSES_CACHE_KEY);
            log.debug("Invalidated all addresses cache");
        } catch (Exception e) {
            log.warn("Error invalidating addresses cache", e);
        }
    }

    private void invalidateCustomerAddressesCache(String customerId) {
        if (customerId != null) {
            try {
                String customerCacheKey = CUSTOMER_ADDRESSES_CACHE_KEY_PREFIX + customerId;
                redisService.delete(customerCacheKey);
                log.debug("Invalidated customer addresses cache for customer: {}", customerId);
            } catch (Exception e) {
                log.warn("Error invalidating customer addresses cache", e);
            }
        }
    }
}