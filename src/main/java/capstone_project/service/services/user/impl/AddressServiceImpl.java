package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.user.AddressRequest;
import capstone_project.dtos.response.trackasiamap.TrackAsiaSearchResponse;
import capstone_project.dtos.response.trackasiamap.TrackAsiaSearchResult;
import capstone_project.dtos.response.user.GeocodingResponse;
import capstone_project.dtos.response.user.AddressResponse;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.user.AddressEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.ThirdPartyServices.TrackAsiaMap.TrackAsiaMapService;
import capstone_project.service.mapper.user.AddressMapper;
import capstone_project.service.services.redis.RedisService;
import capstone_project.service.services.user.AddressService;
import capstone_project.common.utils.AddressUtil;
import capstone_project.common.utils.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressEntityService addressEntityService;
    private final CustomerEntityService customerEntityService;
    private final AddressMapper addressMapper;
    private final VietMapGeocodingServiceImpl geocodingService;
    private final RedisService redisService;
    private final UserContextUtils userContextUtils;
    private final TrackAsiaMapService trackAsiaMapService;

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
            AddressResponse locationData = enhancedCalculateLatLong(fullAddress);

            AddressEntity addressEntity = addressMapper.mapRequestToAddressEntity(request);
            AddressUtil.setCoordinatesOnEntity(addressEntity, locationData.latitude(), locationData.longitude());

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

            // Invalidate caches
            invalidateAddressCaches();
            invalidateCustomerAddressesCache(currentCustomerId.toString());

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
        AddressResponse locationData = enhancedCalculateLatLong(fullAddress);

        AddressUtil.setCoordinatesOnEntity(existing, locationData.latitude(), locationData.longitude());

        // Get current customer ID from security context
        UUID currentCustomerId = userContextUtils.getCurrentCustomerId();

        // 5. Save the updated entity
        AddressEntity updated = addressEntityService.save(existing);
        AddressResponse response = safeMapToResponse(updated);

        // 6. Invalidate caches
        invalidateAddressCaches();
        invalidateCustomerAddressesCache(oldCustomerId);
        invalidateCustomerAddressesCache(currentCustomerId.toString());

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

    @Override
    public List<AddressResponse> getMyDeliveryAddress() {
        UUID customerId = userContextUtils.getCurrentCustomerId();
        log.info("Fetching delivery addresses for current customer: {}", customerId);

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

    private AddressResponse enhancedCalculateLatLong(String address) {
        log.info("Calculating lat/long for address: {}", address);

        // weights
        final double ROOFTOP_BOOST = 2.5;
        final double EXACT_NAME_BOOST = 6.0;
        final double SUBSTRING_BOOST = 2.0;
        final double COMPACT_ALNUM_BOOST = 3.0;
        final double JACCARD_WEIGHT = 3.0;
        final double PLACE_TYPE_BOOST = 1.2;
        final double GEOMETRY_SMALL_BOOST = 0.01;
        final double FUZZY_BOOST = 1.4;
        final double ADMIN_PENALTY_IF_PLACE_QUERY = -2.0;

        // new: street priority weights
        final double STREET_MATCH_BOOST = 8.0;
        final double NO_STREET_PENALTY = -2.5;

        // Vietnamese + generic place keywords (no brands)
        final Set<String> PLACE_KEYWORDS = new LinkedHashSet<>(List.of(
                "phố", "pho", "đường", "duong", "đ", "du", "ngõ", "ngo", "ngach", "số", "so",
                "tổ", "to", "thôn", "khu", "khu dân cư", "kdc", "khu dân", "khu dan cu",
                "tòa", "toa", "tòa nhà", "toa nha", "tang", "tầng", "lầu", "lau",
                "phường", "xã", "phuong", "quận", "quan", "huyện", "huyen",
                "tỉnh", "tinh", "thành phố", "thanh pho", "tp", "khu công nghiệp", "kcn",
                "khu vực", "khu vuc", "chung cư", "chung cu", "block", "tower",
                "apartment", "house", "street", "road", "route", "ward", "district", "city",
                "premise", "establishment", "point_of_interest", "locality", "sublocality"
        ));

        Function<String, String> normalize = s -> {
            if (s == null) return "";
            String tmp = s.replace('Đ', 'D').replace('đ', 'd');
            tmp = java.text.Normalizer.normalize(tmp, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            tmp = tmp.replaceAll("[^\\p{L}\\p{N}\\s]", "");
            tmp = tmp.toLowerCase(java.util.Locale.forLanguageTag("vi")).trim().replaceAll("\\s+", " ");
            String[] toks = tmp.isEmpty() ? new String[0] : tmp.split(" ");
            for (int i = 0; i < toks.length; i++) {
                String t = toks[i];
                if (t.length() > 3 && t.endsWith("s")) t = t.substring(0, t.length() - 1);
                if ("tp".equals(t)) t = "thanh pho";
                if ("kdc".equals(t)) t = "khu dan cu";
                toks[i] = t;
            }
            return String.join(" ", toks);
        };

        java.util.function.BiFunction<String, String, Integer> levenshtein = (a, b) -> {
            if (a == null) return b == null ? 0 : b.length();
            if (b == null) return a.length();
            int[] prev = new int[b.length() + 1];
            for (int j = 0; j <= b.length(); j++) prev[j] = j;
            for (int i = 1; i <= a.length(); i++) {
                int[] cur = new int[b.length() + 1];
                cur[0] = i;
                for (int j = 1; j <= b.length(); j++) {
                    int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                    cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
                }
                prev = cur;
            }
            return prev[b.length()];
        };

        try {
            TrackAsiaSearchResponse searchResp = trackAsiaMapService.search(address);
            if (searchResp == null || searchResp.getResults() == null || searchResp.getResults().isEmpty()) {
                return AddressUtil.buildFallbackResponse(address);
            }

            List<TrackAsiaSearchResult> results = searchResp.getResults();
            String queryNorm = normalize.apply(address);
            String queryCompact = queryNorm.replaceAll("[^\\p{Alnum}]", "");
            Set<String> qTokens = new HashSet<>();
            if (!queryNorm.isEmpty()) Collections.addAll(qTokens, queryNorm.split(" "));

            // extract street portion (before first comma) and normalize
            String streetPart = "";
            if (address != null) {
                int idx = address.indexOf(",");
                streetPart = idx >= 0 ? address.substring(0, idx).trim() : address.trim();
            }
            String streetNorm = normalize.apply(streetPart);
            Set<String> streetTokens = new HashSet<>();
            if (!streetNorm.isEmpty()) Collections.addAll(streetTokens, streetNorm.split(" "));

            // heuristics: determine if query likely targets a specific place/unit/street (Vietnamese-aware)
            boolean queryLooksLikePlace = false;
            java.util.regex.Pattern alphaNumMix = java.util.regex.Pattern.compile(".*[A-Za-z].*\\d.*|.*\\d.*[A-Za-z].*");
            for (String t : qTokens) {
                if (PLACE_KEYWORDS.contains(t) || t.matches("s\\d+.*") || t.matches("\\d+[a-zA-Z]?.*") || alphaNumMix.matcher(t).matches()) {
                    queryLooksLikePlace = true;
                    break;
                }
            }

            Map<TrackAsiaSearchResult, Double> scores = new HashMap<>();
            Map<TrackAsiaSearchResult, Boolean> streetMatchMap = new HashMap<>();

            for (var r : results) {
                String name = r.getName() == null ? "" : r.getName();
                String formatted = "";
                try { formatted = r.getFormattedAddress() == null ? "" : r.getFormattedAddress(); } catch (Exception ignored) {}

                StringBuilder compSb = new StringBuilder();
                try {
                    if (r.getAddressComponents() != null) {
                        for (var c : r.getAddressComponents()) {
                            if (c == null) continue;
                            if (c.getShortName() != null) compSb.append(" ").append(c.getShortName());
                            if (c.getLongName() != null) compSb.append(" ").append(c.getLongName());
                            if (c.getTypes() != null && !c.getTypes().isEmpty()) compSb.append(" ").append(String.join(" ", c.getTypes()));
                        }
                    }
                } catch (Exception ignored) {}

                String typesJoined = "";
                try {
                    if (r.getTypes() != null && !r.getTypes().isEmpty()) typesJoined = String.join(" ", r.getTypes());
                } catch (Exception ignored) {}

                String combined = (name + " " + formatted + " " + compSb.toString() + " " + typesJoined).trim();
                String nameNorm = normalize.apply(name);
                String combinedNorm = normalize.apply(combined);
                String combinedCompact = combinedNorm.replaceAll("[^\\p{Alnum}]", "");

                double score = 0.0;

                try {
                    if (r.getGeometry() != null && r.getGeometry().getLocationType() != null &&
                            "ROOFTOP".equalsIgnoreCase(r.getGeometry().getLocationType())) {
                        score += ROOFTOP_BOOST;
                    }
                } catch (Exception ignored) {}

                boolean candidateIsPlace = false;
                boolean candidateIsAdmin = false;
                try {
                    if (r.getTypes() != null) {
                        for (String t : r.getTypes()) {
                            if (t == null) continue;
                            String low = t.toLowerCase(java.util.Locale.forLanguageTag("vi"));
                            if (low.contains("administrative_area") || low.contains("political") || low.contains("country") || low.contains("region")) candidateIsAdmin = true;
                            if (low.contains("establishment") || low.contains("point_of_interest") || low.contains("route") ||
                                    low.contains("premise") || low.contains("locality") || low.contains("neighborhood") ||
                                    low.contains("sublocality") || low.contains("street_number") || low.contains("school") ||
                                    low.contains("hospital") || low.contains("park") || low.contains("mall") || low.contains("store") ||
                                    low.contains("restaurant") || low.contains("hotel")) candidateIsPlace = true;
                        }
                    }
                } catch (Exception ignored) {}

                if (candidateIsPlace) score += PLACE_TYPE_BOOST;

                String combinedLower = combinedNorm.toLowerCase(java.util.Locale.forLanguageTag("vi"));
                for (String pk : PLACE_KEYWORDS) {
                    if (combinedLower.contains(pk)) {
                        candidateIsPlace = true;
                        score += 0.35;
                    }
                }
                for (String tok : combinedLower.split(" ")) {
                    if (alphaNumMix.matcher(tok).matches()) {
                        candidateIsPlace = true;
                        score += 0.25;
                        break;
                    }
                }

                if (!nameNorm.isEmpty() && nameNorm.equals(queryNorm)) score += EXACT_NAME_BOOST;
                else if (!combinedNorm.isEmpty() && combinedNorm.equals(queryNorm)) score += (EXACT_NAME_BOOST - 1.0);

                if (!nameNorm.isEmpty() && !queryNorm.isEmpty() && (nameNorm.contains(queryNorm) || queryNorm.contains(nameNorm))) score += SUBSTRING_BOOST;
                if (!combinedNorm.isEmpty() && !queryNorm.isEmpty() && (combinedNorm.contains(queryNorm) || queryNorm.contains(combinedNorm))) score += SUBSTRING_BOOST;

                if (!queryCompact.isEmpty() && !combinedCompact.isEmpty()) {
                    if (combinedCompact.contains(queryCompact) || queryCompact.contains(combinedCompact)) score += COMPACT_ALNUM_BOOST;
                }

                Set<String> nTokens = new HashSet<>();
                if (!combinedNorm.isEmpty()) Collections.addAll(nTokens, combinedNorm.split(" "));
                if (!qTokens.isEmpty() && !nTokens.isEmpty()) {
                    Set<String> inter = new HashSet<>(qTokens);
                    inter.retainAll(nTokens);
                    Set<String> uni = new HashSet<>(qTokens);
                    uni.addAll(nTokens);
                    double jaccard = (uni.isEmpty() ? 0.0 : (double) inter.size() / uni.size());
                    score += jaccard * JACCARD_WEIGHT;
                }

                // address component matches influence
                try {
                    if (!qTokens.isEmpty() && r.getAddressComponents() != null) {
                        int compMatches = 0;
                        for (var c : r.getAddressComponents()) {
                            if (c == null) continue;
                            String compText = normalize.apply((c.getShortName() == null ? "" : c.getShortName()) + " " +
                                    (c.getLongName() == null ? "" : c.getLongName()) + " " +
                                    (c.getTypes() == null ? "" : String.join(" ", c.getTypes())));
                            if (compText.isEmpty()) continue;
                            for (String qt : qTokens) if (compText.contains(qt)) compMatches++;
                        }
                        score += Math.min(3.0, compMatches * 0.6);
                    }
                } catch (Exception ignored) {}

                if (combined.length() > 0) score += Math.min(0.6, Math.log1p(combined.length()) / 10.0);

                try {
                    if (r.getGeometry() != null && r.getGeometry().getLocation() != null) score += GEOMETRY_SMALL_BOOST;
                } catch (Exception ignored) {}

                if (candidateIsPlace && queryLooksLikePlace) score += 1.0;
                if (candidateIsAdmin && queryLooksLikePlace) score += ADMIN_PENALTY_IF_PLACE_QUERY;

                double currentJaccard = 0.0;
                try {
                    Set<String> nTokForJ = new HashSet<>();
                    if (!combinedNorm.isEmpty()) Collections.addAll(nTokForJ, combinedNorm.split(" "));
                    Set<String> qTokForJ = new HashSet<>();
                    if (!queryNorm.isEmpty()) Collections.addAll(qTokForJ, queryNorm.split(" "));
                    Set<String> inter = new HashSet<>(qTokForJ);
                    inter.retainAll(nTokForJ);
                    Set<String> uni = new HashSet<>(qTokForJ);
                    uni.addAll(nTokForJ);
                    currentJaccard = (uni.isEmpty() ? 0.0 : (double) inter.size() / uni.size());
                } catch (Exception ignored) {}

                if (currentJaccard < 0.35 && !qTokens.isEmpty() && !nTokens.isEmpty()) {
                    String[] qTs = queryNorm.isEmpty() ? new String[0] : queryNorm.split(" ");
                    String[] nTs = combinedNorm.isEmpty() ? new String[0] : combinedNorm.split(" ");
                    double bestRatio = 1.0;
                    for (String qt : qTs) {
                        for (String nt : nTs) {
                            int dist = levenshtein.apply(qt, nt);
                            int max = Math.max(1, Math.max(qt.length(), nt.length()));
                            double ratio = (double) dist / max;
                            if (ratio < bestRatio) bestRatio = ratio;
                        }
                    }
                    if (bestRatio <= 0.3) score += FUZZY_BOOST;
                }

                // new: street match detection
                boolean streetMatched = false;
                try {
                    if (!streetTokens.isEmpty() && r.getAddressComponents() != null) {
                        for (var c : r.getAddressComponents()) {
                            if (c == null) continue;
                            // check component types for route/street clues
                            boolean typeIndicatesStreet = false;
                            if (c.getTypes() != null) {
                                for (String t : c.getTypes()) {
                                    if (t == null) continue;
                                    String low = t.toLowerCase(java.util.Locale.forLanguageTag("vi"));
                                    if (low.contains("route") || low.contains("street") || low.contains("street_number") || low.contains("street_address")) {
                                        typeIndicatesStreet = true;
                                        break;
                                    }
                                }
                            }
                            String compText = normalize.apply((c.getShortName() == null ? "" : c.getShortName()) + " " +
                                    (c.getLongName() == null ? "" : c.getLongName()));
                            // if types indicate street and comp contains street tokens -> strong match
                            for (String st : streetTokens) {
                                if (typeIndicatesStreet && !compText.isEmpty() && compText.contains(st)) {
                                    streetMatched = true;
                                    break;
                                }
                                // fallback: component text contains street text
                                if (!compText.isEmpty() && compText.contains(st)) {
                                    streetMatched = true;
                                    break;
                                }
                            }
                            if (streetMatched) break;
                        }
                    }
                    // also consider name/formatted containing street as a match
                    if (!streetMatched && !streetNorm.isEmpty()) {
                        if ((name != null && normalize.apply(name).contains(streetNorm)) ||
                                (!formatted.isEmpty() && normalize.apply(formatted).contains(streetNorm))) {
                            streetMatched = true;
                        }
                    }
                } catch (Exception ignored) {}

                if (streetMatched) {
                    score += STREET_MATCH_BOOST;
                } else {
                    // penalize candidates that do not match street when query includes a street part
                    if (!streetTokens.isEmpty()) score += NO_STREET_PENALTY;
                }

                scores.put(r, score);
                streetMatchMap.put(r, streetMatched);
            }

            var sorted = scores.entrySet().stream()
                    .sorted((a, b) -> {
                        // prefer those that match street
                        boolean aStreet = Boolean.TRUE.equals(streetMatchMap.get(a.getKey()));
                        boolean bStreet = Boolean.TRUE.equals(streetMatchMap.get(b.getKey()));
                        if (aStreet && !bStreet) return -1;
                        if (!aStreet && bStreet) return 1;
                        int cmp = Double.compare(b.getValue(), a.getValue());
                        if (cmp != 0) return cmp;
                        String aCombined = normalize.apply((a.getKey().getName() == null ? "" : a.getKey().getName())
                                + " " + (a.getKey().getFormattedAddress() == null ? "" : a.getKey().getFormattedAddress()));
                        String bCombined = normalize.apply((b.getKey().getName() == null ? "" : b.getKey().getName())
                                + " " + (b.getKey().getFormattedAddress() == null ? "" : b.getKey().getFormattedAddress()));
                        return Integer.compare(bCombined.length(), aCombined.length());
                    })
                    .toList();

            if (log.isDebugEnabled()) {
                int top = Math.min(3, sorted.size());
                for (int i = 0; i < top; i++) {
                    var e = sorted.get(i);
                    log.debug("Candidate[{}] name='{}' score={}", i, e.getKey().getName(), e.getValue());
                }
            }

            if (!sorted.isEmpty()) {
                var chosenEntry = sorted.get(0);
                var chosen = chosenEntry.getKey();
                double chosenScore = chosenEntry.getValue();

                // Safely extract formatted, types and components for logging
                String chosenFormatted = "";
                try { chosenFormatted = chosen.getFormattedAddress() == null ? "" : chosen.getFormattedAddress(); } catch (Exception ignored) {}

                String typesStr = "";
                try {
                    if (chosen.getTypes() != null && !chosen.getTypes().isEmpty()) {
                        typesStr = String.join(",", chosen.getTypes());
                    }
                } catch (Exception ignored) {}

                String componentsStr = "";
                try {
                    if (chosen.getAddressComponents() != null && !chosen.getAddressComponents().isEmpty()) {
                        componentsStr = chosen.getAddressComponents().stream()
                                .filter(Objects::nonNull)
                                .map(c -> (c.getShortName() == null ? "" : c.getShortName()) + "/" + (c.getLongName() == null ? "" : c.getLongName()))
                                .collect(java.util.stream.Collectors.joining("; "));
                    }
                } catch (Exception ignored) {}

                // Log which candidate was chosen
                log.info("Chosen TrackAsia candidate index=0 name='{}' score={} formatted='{}' types='{}' components='{}'",
                        chosen.getName(), chosenScore, chosenFormatted, typesStr, componentsStr);

                BigDecimal lat = null, lng = null;
                try {
                    if (chosen.getGeometry() != null && chosen.getGeometry().getLocation() != null) {
                        try {
                            lat = chosen.getGeometry().getLocation().getLat();
                        } catch (Exception ignored) {}
                        try {
                            lng = chosen.getGeometry().getLocation().getLng();
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}

                if (lat != null && lng != null) {
                    AddressResponse response = new AddressResponse(
                            null,
                            null,
                            null,
                            chosen.getName() != null ? chosen.getName() : address,
                            null,
                            lat,
                            lng,
                            null
                    );
                    log.info("Resolved coordinates via TrackAsia - Lat: {}, Long: {}", lat, lng);
                    return response;
                }
            }
        } catch (Exception e) {
            log.warn("Error using TrackAsia search", e);
        }

        return AddressUtil.buildFallbackResponse(address);
    }
}
