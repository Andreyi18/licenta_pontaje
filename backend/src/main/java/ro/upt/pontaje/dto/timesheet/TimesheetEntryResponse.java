package ro.upt.pontaje.dto.timesheet;

import ro.upt.pontaje.model.HourType;
import ro.upt.pontaje.model.TimesheetEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pentru răspunsul cu informații intrare pontaj
 */
public class TimesheetEntryResponse {
    public TimesheetEntryResponse() {}
    public TimesheetEntryResponse(UUID id, UUID timesheetId, LocalDate entryDate, String dayOfWeek, String timeSlot, String startTime, String endTime, HourType hourType, String hourTypeDisplay, String hourTypeColor, String activity, int durationHours, LocalDateTime createdAt) {
        this.id = id;
        this.timesheetId = timesheetId;
        this.entryDate = entryDate;
        this.dayOfWeek = dayOfWeek;
        this.timeSlot = timeSlot;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hourType = hourType;
        this.hourTypeDisplay = hourTypeDisplay;
        this.hourTypeColor = hourTypeColor;
        this.activity = activity;
        this.durationHours = durationHours;
        this.createdAt = createdAt;
    }

    public static class Builder {
        private UUID id;
        private UUID timesheetId;
        private LocalDate entryDate;
        private String dayOfWeek;
        private String timeSlot;
        private String startTime;
        private String endTime;
        private HourType hourType;
        private String hourTypeDisplay;
        private String hourTypeColor;
        private String activity;
        private int durationHours;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder timesheetId(UUID timesheetId) { this.timesheetId = timesheetId; return this; }
        public Builder entryDate(LocalDate entryDate) { this.entryDate = entryDate; return this; }
        public Builder dayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; return this; }
        public Builder timeSlot(String timeSlot) { this.timeSlot = timeSlot; return this; }
        public Builder startTime(String startTime) { this.startTime = startTime; return this; }
        public Builder endTime(String endTime) { this.endTime = endTime; return this; }
        public Builder hourType(HourType hourType) { this.hourType = hourType; return this; }
        public Builder hourTypeDisplay(String hourTypeDisplay) { this.hourTypeDisplay = hourTypeDisplay; return this; }
        public Builder hourTypeColor(String hourTypeColor) { this.hourTypeColor = hourTypeColor; return this; }
        public Builder activity(String activity) { this.activity = activity; return this; }
        public Builder durationHours(int durationHours) { this.durationHours = durationHours; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TimesheetEntryResponse build() {
            return new TimesheetEntryResponse(id, timesheetId, entryDate, dayOfWeek, timeSlot, startTime, endTime, hourType, hourTypeDisplay, hourTypeColor, activity, durationHours, createdAt);
        }
    }

    public static Builder builder() { return new Builder(); }

    // === Getters ===
    public UUID getId() { return id; }
    public UUID getTimesheetId() { return timesheetId; }
    public LocalDate getEntryDate() { return entryDate; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getTimeSlot() { return timeSlot; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public HourType getHourType() { return hourType; }
    public String getHourTypeDisplay() { return hourTypeDisplay; }
    public String getHourTypeColor() { return hourTypeColor; }
    public String getActivity() { return activity; }
    public int getDurationHours() { return durationHours; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private UUID id;
    private UUID timesheetId;
    private LocalDate entryDate;
    private String dayOfWeek;
    private String timeSlot;
    private String startTime;
    private String endTime;
    private HourType hourType;
    private String hourTypeDisplay;
    private String hourTypeColor;
    private String activity;
    private int durationHours;
    private LocalDateTime createdAt;

    public static TimesheetEntryResponse fromEntity(TimesheetEntry entry) {
        return TimesheetEntryResponse.builder()
                .id(entry.getId())
                .timesheetId(entry.getTimesheet().getId())
                .entryDate(entry.getEntryDate())
                .dayOfWeek(entry.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, 
                          new java.util.Locale("ro", "RO")))
                .timeSlot(entry.getTimeSlot())
                .startTime(entry.getStartTime())
                .endTime(entry.getEndTime())
                .hourType(entry.getHourType())
                .hourTypeDisplay(entry.getHourType().getDisplayName())
                .hourTypeColor(entry.getHourType().getColorCode())
                .activity(entry.getActivity())
                .durationHours(entry.getDurationHours())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
