package ro.upt.pontaje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.upt.pontaje.model.DayOfWeek;
import ro.upt.pontaje.model.Schedule;

import java.util.List;
import java.util.UUID;

/**
 * Repository pentru operații CRUD pe entitatea Schedule
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    /**
     * Găsește toate intrările din orar pentru un utilizator
     */
    List<Schedule> findByUserIdOrderByDayOfWeekAscTimeSlotAsc(UUID userId);

    /**
     * Găsește intrările din orar pentru un utilizator și o zi
     */
    List<Schedule> findByUserIdAndDayOfWeekOrderByTimeSlotAsc(UUID userId, DayOfWeek dayOfWeek);

    /**
     * Găsește toate intrările pentru o disciplină
     */
    List<Schedule> findByDisciplineContainingIgnoreCase(String discipline);

    /**
     * Verifică dacă există conflict de orar (același utilizator, zi și interval)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
           "WHERE s.user.id = :userId AND s.dayOfWeek = :dayOfWeek AND s.timeSlot = :timeSlot AND s.id != :excludeId")
    boolean hasTimeConflict(@Param("userId") UUID userId, 
                           @Param("dayOfWeek") DayOfWeek dayOfWeek, 
                           @Param("timeSlot") String timeSlot,
                           @Param("excludeId") UUID excludeId);

    /**
     * Verifică dacă există conflict de orar (pentru creare nouă)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Schedule s " +
           "WHERE s.user.id = :userId AND s.dayOfWeek = :dayOfWeek AND s.timeSlot = :timeSlot")
    boolean hasTimeConflict(@Param("userId") UUID userId, 
                           @Param("dayOfWeek") DayOfWeek dayOfWeek, 
                           @Param("timeSlot") String timeSlot);

    /**
     * Șterge toate intrările din orar pentru un utilizator
     */
    void deleteByUserId(UUID userId);

    /**
     * Numără orele totale din orar pentru un utilizator
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
