package capstone_project.service.services.map.impl;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.service.services.map.VietMapDistanceService;
import capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of VietMapDistanceService using VietMap API
 * This service provides accurate distance calculation considering vehicle type and road conditions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VietMapDistanceServiceImpl implements VietMapDistanceService {

    private final VietmapService vietmapService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String DEFAULT_VEHICLE_TYPE = "car";
    private static final double EARTH_RADIUS_KM = 6371.0; // Fallback for Haversine calculation

    @Override
    public BigDecimal calculateDistance(AddressEntity fromAddress, AddressEntity toAddress, String vehicleType) {
        if (fromAddress == null || toAddress == null) {
            throw new IllegalArgumentException("From and to addresses must not be null");
        }

        Double fromLat = fromAddress.getLatitude() != null ? fromAddress.getLatitude().doubleValue() : null;
        Double fromLng = fromAddress.getLongitude() != null ? fromAddress.getLongitude().doubleValue() : null;
        Double toLat = toAddress.getLatitude() != null ? toAddress.getLatitude().doubleValue() : null;
        Double toLng = toAddress.getLongitude() != null ? toAddress.getLongitude().doubleValue() : null;

        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            throw new IllegalArgumentException("Address coordinates must not be null");
        }

        return calculateDistance(fromLat, fromLng, toLat, toLng, vehicleType);
    }

    @Override
    public BigDecimal calculateDistance(Double fromLat, Double fromLng, Double toLat, Double toLng, String vehicleType) {
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) {
            throw new IllegalArgumentException("Coordinates must not be null");
        }

        log.info("🗺️ Calculating distance from ({}, {}) to ({}, {}) for vehicle type: {}", 
                 fromLat, fromLng, toLat, toLng, vehicleType);

        // Check if coordinates are the same
        if (fromLat.equals(toLat) && fromLng.equals(toLng)) {
            log.info("📍 Same coordinates, distance = 0 km");
            return BigDecimal.ZERO;
        }

        try {
            // Create points list for VietMap API (format: "lat,lng")
            List<String> points = Arrays.asList(
                fromLat + "," + fromLng,
                toLat + "," + toLng
            );

            // Call VietMap route API
            String routeResponse = vietmapService.route(
                points,
                false, // points not encoded
                vehicleType != null ? vehicleType : DEFAULT_VEHICLE_TYPE,
                false, // no optimization
                null   // no avoid
            );

            // Parse JSON response to extract distance
            BigDecimal distance = parseDistanceFromResponse(routeResponse);
            
            log.info("✅ VietMap distance calculated: {} km", distance);
            return distance;

        } catch (Exception e) {
            log.warn("⚠️ VietMap API failed, falling back to Haversine calculation: {}", e.getMessage());
            return fallbackHaversineDistance(fromLat, fromLng, toLat, toLng);
        }
    }

    @Override
    public BigDecimal calculateDistance(AddressEntity fromAddress, AddressEntity toAddress) {
        return calculateDistance(fromAddress, toAddress, DEFAULT_VEHICLE_TYPE);
    }

    /**
     * Parse distance from VietMap route API response
     */
    private BigDecimal parseDistanceFromResponse(String routeResponse) throws Exception {
        JsonNode rootNode = objectMapper.readTree(routeResponse);
        
        // Check if response has paths
        if (rootNode.has("paths") && rootNode.get("paths").isArray() && rootNode.get("paths").size() > 0) {
            JsonNode firstPath = rootNode.get("paths").get(0);
            
            // Distance is in meters, convert to kilometers
            if (firstPath.has("distance")) {
                double distanceMeters = firstPath.get("distance").asDouble();
                BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters / 1000.0)
                    .setScale(2, RoundingMode.HALF_UP);
                
                log.info("📏 Parsed distance from VietMap: {} meters = {} km", distanceMeters, distanceKm);
                return distanceKm;
            }
        }
        
        throw new RuntimeException("Invalid VietMap route response: missing distance data");
    }

    /**
     * Fallback Haversine formula calculation when VietMap API fails
     */
    private BigDecimal fallbackHaversineDistance(Double fromLat, Double fromLng, Double toLat, Double toLng) {
        log.info("🧮 Using Haversine formula fallback");
        
        double dLat = Math.toRadians(toLat - fromLat);
        double dLng = Math.toRadians(toLng - fromLng);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;
        
        BigDecimal result = BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP);
        log.info("📏 Haversine fallback distance: {} km", result);
        
        return result;
    }
}
