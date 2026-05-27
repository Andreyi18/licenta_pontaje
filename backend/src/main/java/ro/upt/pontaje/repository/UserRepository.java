package ro.upt.pontaje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.model.UserRole;
import ro.upt.pontaje.model.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pentru operatii CRUD pe entitatea User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Gaseste un utilizator dupa email
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica daca exista un utilizator cu emailul dat
     */
    boolean existsByEmail(String email);

    /**
     * Gaseste toti utilizatorii dupa rol
     */
    List<User> findByRole(UserRole role);

    /**
     * Gaseste toti utilizatorii dupa status
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Gaseste toti utilizatorii activi dintr-un departament
     */
    List<User> findByDepartmentIdAndStatus(UUID departmentId, UserStatus status);

    /**
     * Gaseste toti utilizatorii dupa rol si status
     */
    List<User> findByRoleAndStatus(UserRole role, UserStatus status);

    /**
     * Gaseste utilizatori dupa nume (parțial, case-insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> searchByName(@Param("name") String name);

    /**
     * Gaseste toti cadrele didactice dintr-un departament
     */
    @Query("SELECT u FROM User u WHERE u.department.id = :departmentId AND u.role = 'CADRU_DIDACTIC' AND u.status = 'ACTIVE'")
    List<User> findActiveCadreDidacticeByDepartment(@Param("departmentId") UUID departmentId);

    /**
     * Numara utilizatorii pe rol
     */
    long countByRole(UserRole role);

    /**
     * Numara utilizatorii activi pe departament
     */
    long countByDepartmentIdAndStatus(UUID departmentId, UserStatus status);
}
