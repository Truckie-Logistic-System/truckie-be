package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends BaseRepository<VehicleEntity> {
    Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber);

    /**
     * Find vehicle by ID with eagerly fetched vehicleType relationship
     */
    @Query("SELECT v FROM VehicleEntity v " +
           "LEFT JOIN FETCH v.vehicleTypeEntity vt " +
           "WHERE v.id = :vehicleId")
    Optional<VehicleEntity> findByIdWithVehicleType(@Param("vehicleId") UUID vehicleId);

    @Query(value = """
        SELECT v.*, va.*,vt.*,vm.*
        FROM vehicles v
        LEFT JOIN vehicle_assignments va ON v.id = va.vehicle_id
        JOIN vehicle_types vt ON v.vehicle_type_id = vt.id
        LEFT JOIN vehicle_maintenance vm ON v.id = vm.vehicle_id
        WHERE v.id = :id
        """, nativeQuery = true)
    Optional<VehicleEntity> findVehicleWithJoinsById(@Param("id") UUID id);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntity(VehicleTypeEntity vehicleTypeEntity);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntityAndStatus(VehicleTypeEntity vehicleTypeEntity, String status);

    List<VehicleEntity> findVehicleEntitiesByVehicleTypeEntityIdAndStatus(UUID vehicleTypeEntityId, String status);

    /**
     * Directly updates the location of a vehicle without loading the entity first
     * @return number of rows affected (should be 1 if successful)
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE vehicles 
        SET current_latitude = :latitude, current_longitude = :longitude, last_updated = :lastUpdated 
        WHERE id = :id AND 
        (current_latitude != :latitude OR current_longitude != :longitude)
        """, nativeQuery = true)
    int updateLocationDirectly(
            @Param("id") UUID id,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("lastUpdated") LocalDateTime lastUpdated);

    /**
     * Same as updateLocationDirectly but adds rate limiting: only updates if last_updated is older than the specified threshold
     * Updates in two cases:
     * 1. Location changed (lat/lng different) - always update regardless of rate limit
     * 2. Location unchanged but last_updated is null or older than threshold - update last_updated to mark as received
     * @return number of rows affected (1 if updated, 0 if skipped due to rate limit)
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE vehicles 
        SET current_latitude = :latitude, current_longitude = :longitude, last_updated = :lastUpdated 
        WHERE id = :id AND 
        (
            (current_latitude != :latitude OR current_longitude != :longitude) OR
            (last_updated IS NULL OR last_updated < :minLastUpdated)
        )
        """, nativeQuery = true)
    int updateLocationWithRateLimit(
            @Param("id") UUID id,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("lastUpdated") LocalDateTime lastUpdated,
            @Param("minLastUpdated") LocalDateTime minLastUpdated);

    /**
     * Batch update locations for multiple vehicles in a single query
     * Each vehicle must be listed individually with their own parameters
     * Example usage: updateBatchLocations(id1, lat1, lng1, id2, lat2, lng2, timestamp)
     * @return number of rows affected
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE vehicles 
        SET 
            current_latitude = CASE id 
                WHEN :id1 THEN :lat1 
                WHEN :id2 THEN :lat2 
                WHEN :id3 THEN :lat3
                WHEN :id4 THEN :lat4
                WHEN :id5 THEN :lat5
                ELSE current_latitude END,
            current_longitude = CASE id 
                WHEN :id1 THEN :lng1 
                WHEN :id2 THEN :lng2
                WHEN :id3 THEN :lng3
                WHEN :id4 THEN :lng4
                WHEN :id5 THEN :lng5
                ELSE current_longitude END,
            last_updated = :lastUpdated
        WHERE id IN (:id1, :id2, :id3, :id4, :id5)
        AND (
            (id = :id1 AND (current_latitude != :lat1 OR current_longitude != :lng1)) OR
            (id = :id2 AND (current_latitude != :lat2 OR current_longitude != :lng2)) OR
            (id = :id3 AND (current_latitude != :lat3 OR current_longitude != :lng3)) OR
            (id = :id4 AND (current_latitude != :lat4 OR current_longitude != :lng4)) OR
            (id = :id5 AND (current_latitude != :lat5 OR current_longitude != :lng5))
        )
        """, nativeQuery = true)
    int updateBatchLocations(
            @Param("id1") UUID id1, @Param("lat1") BigDecimal lat1, @Param("lng1") BigDecimal lng1,
            @Param("id2") UUID id2, @Param("lat2") BigDecimal lat2, @Param("lng2") BigDecimal lng2,
            @Param("id3") UUID id3, @Param("lat3") BigDecimal lat3, @Param("lng3") BigDecimal lng3,
            @Param("id4") UUID id4, @Param("lat4") BigDecimal lat4, @Param("lng4") BigDecimal lng4,
            @Param("id5") UUID id5, @Param("lat5") BigDecimal lat5, @Param("lng5") BigDecimal lng5,
            @Param("lastUpdated") LocalDateTime lastUpdated);
}