package capstone_project.service.services.map;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.service.services.map.impl.VietMapDistanceServiceImpl.DistanceResult;

import java.math.BigDecimal;

/**
 * Service for calculating accurate distances using VietMap API
 * This service considers vehicle type and actual road conditions
 */
public interface VietMapDistanceService {
    
    /**
     * Calculate distance between two addresses using VietMap API
     * @param fromAddress Starting address
     * @param toAddress Destination address
     * @param vehicleType Vehicle type for routing (e.g., "car", "motorcycle", "truck")
     * @return Distance in kilometers
     */
    BigDecimal calculateDistance(AddressEntity fromAddress, AddressEntity toAddress, String vehicleType);
    
    /**
     * Calculate distance between two coordinates using VietMap API
     * @param fromLat Starting latitude
     * @param fromLng Starting longitude
     * @param toLat Destination latitude
     * @param toLng Destination longitude
     * @param vehicleType Vehicle type for routing
     * @return Distance in kilometers
     */
    BigDecimal calculateDistance(Double fromLat, Double fromLng, Double toLat, Double toLng, String vehicleType);
    
    /**
     * Calculate distance with default vehicle type (car)
     * @param fromAddress Starting address
     * @param toAddress Destination address
     * @return Distance in kilometers
     */
    BigDecimal calculateDistance(AddressEntity fromAddress, AddressEntity toAddress);
    
    /**
     * Calculate distance and toll information between two addresses
     * @param fromAddress Starting address
     * @param toAddress Destination address
     * @param vehicleType Vehicle type for routing
     * @return DistanceResult containing distance and toll information
     */
    DistanceResult calculateDistanceAndTolls(AddressEntity fromAddress, AddressEntity toAddress, String vehicleType);
}
