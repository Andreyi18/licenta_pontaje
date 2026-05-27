package ro.upt.pontaje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.upt.pontaje.model.HourType;
import ro.upt.pontaje.model.TimesheetEntry;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository pentru operații CRUD pe entitatea TimesheetEntry
 */
@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, UUID> {

    /**
     * Găsește toate intrările pentru un pontaj
     */
    List<TimesheetEntry> findByTimesheetIdOrderByEntryDateAscTimeSlotAsc(UUID timesheetId);

    /**
     * Găsește intrările pentru un pontaj și o dată specifică
     */
    List<TimesheetEntry> findByTimesheetIdAndEntryDateOrderByTimeSlotAsc(UUID timesheetId, LocalDate entryDate);

    /**
     * Verifică dacă există intrare pentru pontaj, dată și interval
     */
    boolean existsByTimesheetIdAndEntryDateAndTimeSlot(UUID timesheetId, LocalDate entryDate, String timeSlot);

    /**
     * Găsește intrările după tip de oră
     */
    List<TimesheetEntry> findByTimesheetIdAndHourType(UUID timesheetId, HourType hourType);

    /**
     * Șterge toate intrările pentru un pontaj
     */
    void deleteByTimesheetId(UUID timesheetId);

    /**
     * Numără orele pe tip pentru un pontaj
     */
    @Query("SELECT e.hourType, COUNT(e) FROM TimesheetEntry e WHERE e.timesheet.id = :timesheetId GROUP BY e.hourType")
    List<Object[]> countByHourType(@Param("timesheetId") UUID timesheetId);

    /**
     * Găsește intrările pentru o perioadă de date
     */
    @Query("SELECT e FROM TimesheetEntry e WHERE e.timesheet.id = :timesheetId " +
           "AND e.entryDate BETWEEN :startDate AND :endDate ORDER BY e.entryDate ASC, e.timeSlot ASC")
    List<TimesheetEntry> findByDateRange(
        @Param("timesheetId") UUID timesheetId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
