package ro.upt.pontaje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.upt.pontaje.model.Department;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pentru operații CRUD pe entitatea Department
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    /**
     * Găsește un departament după cod
     */
    Optional<Department> findByCode(String code);

    /**
     * Verifică dacă există un departament cu codul dat
     */
    boolean existsByCode(String code);

    /**
     * Găsește un departament după nume (parțial, case-insensitive)
     */
    Optional<Department> findByNameContainingIgnoreCase(String name);
}
