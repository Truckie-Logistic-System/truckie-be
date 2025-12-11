package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.order.order.FuelTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuelTypeRepository extends JpaRepository<FuelTypeEntity, UUID> {
    
    Optional<FuelTypeEntity> findByName(String name);
    
    List<FuelTypeEntity> findByNameContainingIgnoreCase(String name);
    
    boolean existsByName(String name);
    
    boolean existsByNameAndIdNot(String name, UUID id);
}
