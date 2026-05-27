package ro.upt.pontaje.dto.schedule;


import ro.upt.pontaje.model.ActivityType;
import ro.upt.pontaje.model.DayOfWeek;
import ro.upt.pontaje.model.Schedule;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pentru răspunsul cu informații din orar
 */
public class ScheduleResponse {

    private UUID id;
    private UUID userId;
    private DayOfWeek dayOfWeek;
    private String dayOfWeekDisplay;
    private String timeSlot;
    private String startTime;
    private String endTime;
    private String discipline;
    private String room;
    private ActivityType activityType;
    private String activityTypeDisplay;
    private int durationHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScheduleResponse() {}

    public ScheduleResponse(UUID id, UUID userId, DayOfWeek dayOfWeek, String dayOfWeekDisplay, String timeSlot, String startTime, String endTime, String discipline, String room, ActivityType activityType, String activityTypeDisplay, int durationHours, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.dayOfWeekDisplay = dayOfWeekDisplay;
        this.timeSlot = timeSlot;
        this.startTime = startTime;
        this.endTime = endTime;
        this.discipline = discipline;
        this.room = room;
        this.activityType = activityType;
        this.activityTypeDisplay = activityTypeDisplay;
        this.durationHours = durationHours;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private DayOfWeek dayOfWeek;
        private String dayOfWeekDisplay;
        private String timeSlot;
        private String startTime;
        private String endTime;
        private String discipline;
        private String room;
        private ActivityType activityType;
        private String activityTypeDisplay;
        private int durationHours;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder dayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; return this; }
        public Builder dayOfWeekDisplay(String dayOfWeekDisplay) { this.dayOfWeekDisplay = dayOfWeekDisplay; return this; }
        public Builder timeSlot(String timeSlot) { this.timeSlot = timeSlot; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }
        public Builder discipline(String discipline) { this.discipline = discipline; return this; }
        public Builder room(String room) { this.room = room; return this; }
        public Builder activityType(ActivityType activityType) { this.activityType = activityType; return this; }
        public Builder activityTypeDisplay(String activityTypeDisplay) { this.activityTypeDisplay = activityTypeDisplay; return this; }
        public Builder durationHours(int durationHours) { this.durationHours = durationHours; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ScheduleResponse build() {
            return new ScheduleResponse(id, userId, dayOfWeek, dayOfWeekDisplay, timeSlot, startTime, endTime, discipline, room, activityType, activityTypeDisplay, durationHours, createdAt, updatedAt);
        }
    }

    public static Builder builder() { return new Builder(); }

    // === Getters ===
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public String getDayOfWeekDisplay() { return dayOfWeekDisplay; }
    public String getTimeSlot() { return timeSlot; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDiscipline() { return discipline; }
    public String getRoom() { return room; }
    public ActivityType getActivityType() { return activityType; }
    public String getActivityTypeDisplay() { return activityTypeDisplay; }
    public int getDurationHours() { return durationHours; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static ScheduleResponse fromEntity(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .userId(schedule.getUser().getId())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayOfWeekDisplay(schedule.getDayOfWeek().getDisplayName())
                .timeSlot(schedule.getTimeSlot())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .discipline(schedule.getDiscipline())
                .room(schedule.getRoom())
                .activityType(schedule.getActivityType())
                .activityTypeDisplay(schedule.getActivityType().getDisplayName())
                .durationHours(schedule.getDurationHours())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
