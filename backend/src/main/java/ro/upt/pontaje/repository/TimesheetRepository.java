package ro.upt.pontaje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.upt.pontaje.model.Timesheet;
import ro.upt.pontaje.model.TimesheetStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pentru operații CRUD pe entitatea Timesheet
 */
@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {

    /**
     * Găsește toate pontajele pentru un utilizator (cu user+department încărcate eager)
     */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.user u LEFT JOIN FETCH u.department WHERE u.id = :userId ORDER BY t.year DESC, t.month DESC")
    List<Timesheet> findByUserIdOrderByYearDescMonthDesc(@Param("userId") UUID userId);

    /**
     * Găsește pontajul pentru un utilizator, lună și an specific (cu user+department eager)
     */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.user u LEFT JOIN FETCH u.department WHERE u.id = :userId AND t.month = :month AND t.year = :year")
    Optional<Timesheet> findByUserIdAndMonthAndYear(@Param("userId") UUID userId, @Param("month") Integer month, @Param("year") Integer year);

    /**
     * Verifică dacă există pontaj pentru utilizator, lună și an
     */
    boolean existsByUserIdAndMonthAndYear(UUID userId, Integer month, Integer year);

    /**
     * Găsește toate pontajele după status
     */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.user u LEFT JOIN FETCH u.department WHERE t.status = :status")
    List<Timesheet> findByStatus(@Param("status") TimesheetStatus status);

    /**
     * Găsește toate pontajele pentru o lună și an
     */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.user u LEFT JOIN FETCH u.department WHERE t.month = :month AND t.year = :year ORDER BY u.lastName ASC")
    List<Timesheet> findByMonthAndYearOrderByUserLastNameAsc(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Găsește pontajele trimise pentru o lună și an
     */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.user u LEFT JOIN FETCH u.department WHERE t.month = :month AND t.year = :year AND t.status = :status ORDER BY u.lastName ASC")
    List<Timesheet> findByMonthAndYearAndStatusOrderByUserLastNameAsc(
        @Param("month") Integer month, @Param("year") Integer year, @Param("status") TimesheetStatus status);

    /**
     * Găsește pontajele dintr-un departament pentru o lună și an
     */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.user u LEFT JOIN FETCH u.department WHERE u.department.id = :departmentId " +
           "AND t.month = :month AND t.year = :year ORDER BY u.lastName ASC")
    List<Timesheet> findByDepartmentAndPeriod(
        @Param("departmentId") UUID departmentId,
        @Param("month") Integer month,
        @Param("year") Integer year);

    /**
     * Găsește utilizatorii care nu au trimis pontajul pentru o perioadă
     */
    @Query("SELECT u.id FROM User u WHERE u.role = 'CADRU_DIDACTIC' AND u.status = 'ACTIVE' " +
           "AND u.id NOT IN (SELECT t.user.id FROM Timesheet t WHERE t.month = :month AND t.year = :year " +
           "AND t.status IN ('SUBMITTED', 'APPROVED'))")
    List<UUID> findUsersWithoutSubmittedTimesheet(
        @Param("month") Integer month, 
        @Param("year") Integer year);

    /**
     * Statistici pontaje pe status pentru o perioadă
     */
    @Query("SELECT t.status, COUNT(t) FROM Timesheet t WHERE t.month = :month AND t.year = :year GROUP BY t.status")
    List<Object[]> getStatusStatistics(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Numără pontajele trimise pentru o lună și an
     */
    long countByMonthAndYearAndStatus(Integer month, Integer year, TimesheetStatus status);

    /**
     * Găsește un pontaj după id cu user+department eager
     */
    @Query("SELECT t FROM Timesheet t JOIN FETCH t.user u LEFT JOIN FETCH u.department WHERE t.id = :id")
    Optional<Timesheet> findByIdWithUser(@Param("id") UUID id);
}
