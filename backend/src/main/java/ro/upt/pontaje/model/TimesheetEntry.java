package ro.upt.pontaje.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entitatea pentru intrarile individuale din pontaj (ore marcate)
 */
@Entity
@Table(name = "timesheet_entries", indexes = {
    @Index(name = "idx_entries_timesheet", columnList = "timesheet_id"),
    @Index(name = "idx_entries_date", columnList = "entry_date")
})
public class TimesheetEntry {

    // === Constructors ===
    public TimesheetEntry() {}

    public TimesheetEntry(UUID id, Timesheet timesheet, LocalDate entryDate, String timeSlot, HourType hourType, String activity, LocalDateTime createdAt) {
        this.id = id;
        this.timesheet = timesheet;
        this.entryDate = entryDate;
        this.timeSlot = timeSlot;
        this.hourType = hourType;
        this.activity = activity;
        this.createdAt = createdAt;
    }

    // === Getters and Setters ===
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Timesheet getTimesheet() { return timesheet; }
    public void setTimesheet(Timesheet timesheet) { this.timesheet = timesheet; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public HourType getHourType() { return hourType; }
    public void setHourType(HourType hourType) { this.hourType = hourType; }
    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // === Builder ===
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private UUID id;
        private Timesheet timesheet;
        private LocalDate entryDate;
        private String timeSlot;
        private HourType hourType;
        private String activity;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder timesheet(Timesheet timesheet) { this.timesheet = timesheet; return this; }
        public Builder entryDate(LocalDate entryDate) { this.entryDate = entryDate; return this; }
        public Builder timeSlot(String timeSlot) { this.timeSlot = timeSlot; return this; }
        public Builder hourType(HourType hourType) { this.hourType = hourType; return this; }
        public Builder activity(String activity) { this.activity = activity; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TimesheetEntry build() {
            return new TimesheetEntry(id, timesheet, entryDate, timeSlot, hourType, activity, createdAt);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private Timesheet timesheet;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /**
     * Interval orar in format "HH:MM-HH:MM" (ex: "08:00-10:00")
     */
    @Column(name = "time_slot", nullable = false, length = 11)
    private String timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "hour_type", nullable = false, length = 15)
    private HourType hourType;

    /**
     * Descrierea activitatii desfasurate
     */
    @Column(length = 500)
    private String activity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // methode helper

    /**
     * Returneaza ora de inceput din intervalul orar
     */
    public String getStartTime() {
        if (timeSlot != null && timeSlot.contains("-")) {
            return timeSlot.split("-")[0];
        }
        return null;
    }

    /**
     * Returneaza ora de sfarsit din intervalul orar
     */
    public String getEndTime() {
        if (timeSlot != null && timeSlot.contains("-")) {
            return timeSlot.split("-")[1];
        }
        return null;
    }

    /**
     * Calculeaza durata in ore
     */
    public int getDurationHours() {
        try {
            String[] parts = timeSlot.split("-");
            int startHour = Integer.parseInt(parts[0].split(":")[0]);
            int endHour = Integer.parseInt(parts[1].split(":")[0]);
            return endHour - startHour;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returneaza ziua saptamanii pentru data intrarii
     */
    public java.time.DayOfWeek getDayOfWeek() {
        return entryDate.getDayOfWeek();
    }
}
