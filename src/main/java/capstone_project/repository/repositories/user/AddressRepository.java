package capstone_project.repository.repositories.user;

import capstone_project.entity.user.address.AddressEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface AddressRepository extends BaseRepository<AddressEntity> {

    // Phương thức này không hoạt động đúng vì entity không có trường customerId
    // mà có trường customer là một đối tượng CustomerEntity
    List<AddressEntity> findByCustomerId(UUID customerId);
    
    // Phương thức đúng để tìm địa chỉ theo customer ID
    List<AddressEntity> findByCustomer_Id(UUID customerId);
    
    // Hoặc sử dụng query JPQL tùy chỉnh
    @Query("SELECT a FROM AddressEntity a WHERE a.customer.id = :customerId")
    List<AddressEntity> findAllByCustomerId(@Param("customerId") UUID customerId);
}
