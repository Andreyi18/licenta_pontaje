package ro.upt.pontaje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.upt.pontaje.model.AnnexType;
import ro.upt.pontaje.model.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pentru operații CRUD pe entitatea Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Găsește toate documentele pentru un utilizator
     */
    List<Document> findByUserIdOrderByGeneratedAtDesc(UUID userId);

    /**
     * Găsește documentele pentru un pontaj
     */
    List<Document> findByTimesheetId(UUID timesheetId);

    /**
     * Găsește documentul specific (pontaj + tip anexă)
     */
    Optional<Document> findByTimesheetIdAndAnnexType(UUID timesheetId, AnnexType annexType);

    /**
     * Găsește documentele pentru o lună și an (pentru secretariat)
     */
    @Query("SELECT d FROM Document d WHERE d.timesheet.month = :month AND d.timesheet.year = :year " +
           "ORDER BY d.user.lastName ASC, d.annexType ASC")
    List<Document> findByPeriod(@Param("month") Integer month, @Param("year") Integer year);

    /**
     * Găsește documentele dintr-un departament pentru o perioadă
     */
    @Query("SELECT d FROM Document d WHERE d.user.department.id = :departmentId " +
           "AND d.timesheet.month = :month AND d.timesheet.year = :year " +
           "ORDER BY d.user.lastName ASC, d.annexType ASC")
    List<Document> findByDepartmentAndPeriod(
        @Param("departmentId") UUID departmentId,
        @Param("month") Integer month,
        @Param("year") Integer year);

    /**
     * Verifică dacă există document pentru pontaj și tip anexă
     */
    boolean existsByTimesheetIdAndAnnexType(UUID timesheetId, AnnexType annexType);
}
