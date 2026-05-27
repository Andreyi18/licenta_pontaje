package ro.upt.pontaje.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entitatea pentru intrarile din orarul saptamanal
 */
@Entity
@Table(name = "schedules", indexes = {
    @Index(name = "idx_schedules_user", columnList = "user_id"),
    @Index(name = "idx_schedules_day", columnList = "day_of_week")
})
public class Schedule {

    // === Constructors ===
    public Schedule() {}

    public Schedule(UUID id, User user, DayOfWeek dayOfWeek, String timeSlot, String discipline, String room, ActivityType activityType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.dayOfWeek = dayOfWeek;
        this.timeSlot = timeSlot;
        this.discipline = discipline;
        this.room = room;
        this.activityType = activityType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // === Getters and Setters ===
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // === Builder ===
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private UUID id;
        private User user;
        private DayOfWeek dayOfWeek;
        private String timeSlot;
        private String discipline;
        private String room;
        private ActivityType activityType;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder dayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; return this; }
        public Builder timeSlot(String timeSlot) { this.timeSlot = timeSlot; return this; }
        public Builder discipline(String discipline) { this.discipline = discipline; return this; }
        public Builder room(String room) { this.room = room; return this; }
        public Builder activityType(ActivityType activityType) { this.activityType = activityType; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Schedule build() {
            return new Schedule(id, user, dayOfWeek, timeSlot, discipline, room, activityType, createdAt, updatedAt);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    /**
     * Interval orar in format "HH:MM-HH:MM" (ex: "08:00-10:00")
     */
    @Column(name = "time_slot", nullable = false, length = 11)
    private String timeSlot;

    @Column(nullable = false, length = 200)
    private String discipline;

    @Column(length = 50)
    private String room;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 15)
    private ActivityType activityType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // methode helper

    /**
     * Returneaza ora de început din intervalul orar
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
}
