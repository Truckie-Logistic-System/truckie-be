package capstone_project.service.entityServices;

import java.util.List;
import java.util.Optional;

public interface BaseEntityService<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();
//    void deleteById(ID id);
}