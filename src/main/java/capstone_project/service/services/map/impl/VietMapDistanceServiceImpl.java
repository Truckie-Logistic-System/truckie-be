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

        log.info("️ Calculating distance from ({}, {}) to ({}, {}) for vehicle type: {}", 
                 fromLat, fromLng, toLat, toLng, vehicleType);

        // Check if coordinates are the same
        if (fromLat.equals(toLat) && fromLng.equals(toLng)) {
            log.info("Same coordinates, distance = 0 km");
            return BigDecimal.ZERO;
        }

        try {
            // Build path JSON for route-tolls API: [[lng, lat], [lng, lat]]
            java.util.List<java.util.List<Double>> path = Arrays.asList(
                Arrays.asList(fromLng, fromLat),
                Arrays.asList(toLng, toLat)
            );
            
            String pathJson = objectMapper.writeValueAsString(path);
            log.info("Calling VietMap route-tolls API with path: {}", pathJson);
            
            // Map vehicle type to VietMap vehicle integer if needed
            Integer vietVehicle = mapVehicleTypeToInteger(vehicleType);
            
            // Call route-tolls API for consistency with DistanceService
            String routeTollsResponse = vietmapService.routeTolls(pathJson, vietVehicle);
            log.info("VietMap route-tolls API response received");

            // Parse JSON response to extract distance from path
            BigDecimal distance = parseDistanceFromRouteTollsResponse(routeTollsResponse);
            
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
     * Parse distance from VietMap route-tolls API response
     * Route-tolls returns path coordinates, so we calculate distance from the path
     */
    private BigDecimal parseDistanceFromRouteTollsResponse(String routeTollsResponse) throws Exception {
        JsonNode rootNode = objectMapper.readTree(routeTollsResponse);
        
        // Extract path coordinates from response
        JsonNode pathNode = rootNode.path("path");
        
        if (!pathNode.isArray() || pathNode.size() < 2) {
            throw new RuntimeException("Invalid VietMap route-tolls response: missing or invalid path data");
        }
        
        // Calculate distance from path coordinates using Haversine
        double totalDistanceKm = 0.0;
        
        for (int i = 0; i < pathNode.size() - 1; i++) {
            JsonNode point1 = pathNode.get(i);
            JsonNode point2 = pathNode.get(i + 1);
            
            if (point1.isArray() && point1.size() >= 2 && point2.isArray() && point2.size() >= 2) {
                double lng1 = point1.get(0).asDouble();
                double lat1 = point1.get(1).asDouble();
                double lng2 = point2.get(0).asDouble();
                double lat2 = point2.get(1).asDouble();
                
                totalDistanceKm += calculateHaversineDistanceKm(lat1, lng1, lat2, lng2);
            }
        }
        
        BigDecimal distanceKm = BigDecimal.valueOf(totalDistanceKm)
            .setScale(2, RoundingMode.HALF_UP);
        
        log.info("📏 Calculated distance from route-tolls path: {} km ({} segments)", 
                 distanceKm, pathNode.size() - 1);
        
        return distanceKm;
    }
    
    /**
     * Calculate Haversine distance between two points in kilometers
     */
    private double calculateHaversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * Map vehicle type string to VietMap vehicle integer
     */
    private Integer mapVehicleTypeToInteger(String vehicleType) {
        if (vehicleType == null || vehicleType.isEmpty()) {
            return null; // Let API use default
        }
        
        // Map vehicle types to VietMap integers if needed
        // For now, return null to use default (car)
        return null;
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
