// ...existing code...
package ro.upt.pontaje.dto.timesheet;

import ro.upt.pontaje.model.Timesheet;
import ro.upt.pontaje.model.TimesheetStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO pentru răspunsul cu informații pontaj
 */
public class TimesheetResponse {
    // === Getters ===
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getDepartmentName() { return departmentName; }
    public Integer getMonth() { return month; }
    public Integer getYear() { return year; }
    public String getPeriodDisplay() { return periodDisplay; }
    public TimesheetStatus getStatus() { return status; }
    public String getStatusDisplay() { return statusDisplay; }
    public int getTotalNormaHours() { return totalNormaHours; }
    public int getTotalPlataOraHours() { return totalPlataOraHours; }
    public int getTotalHours() { return totalHours; }
    public List<TimesheetEntryResponse> getEntries() { return entries; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isEditable() { return editable; }

    public TimesheetResponse() {}
    public TimesheetResponse(UUID id, UUID userId, String userName, String userEmail, String departmentName, Integer month, Integer year, String periodDisplay, TimesheetStatus status, String statusDisplay, int totalNormaHours, int totalPlataOraHours, int totalHours, List<TimesheetEntryResponse> entries, LocalDateTime submittedAt, LocalDateTime createdAt, LocalDateTime updatedAt, boolean editable) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.departmentName = departmentName;
        this.month = month;
        this.year = year;
        this.periodDisplay = periodDisplay;
        this.status = status;
        this.statusDisplay = statusDisplay;
        this.totalNormaHours = totalNormaHours;
        this.totalPlataOraHours = totalPlataOraHours;
        this.totalHours = totalHours;
        this.entries = entries;
        this.submittedAt = submittedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.editable = editable;
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private String userName;
        private String userEmail;
        private String departmentName;
        private Integer month;
        private Integer year;
        private String periodDisplay;
        private TimesheetStatus status;
        private String statusDisplay;
        private int totalNormaHours;
        private int totalPlataOraHours;
        private int totalHours;
        private List<TimesheetEntryResponse> entries;
        private LocalDateTime submittedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean editable;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder userName(String userName) { this.userName = userName; return this; }
        public Builder userEmail(String userEmail) { this.userEmail = userEmail; return this; }
        public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
        public Builder month(Integer month) { this.month = month; return this; }
        public Builder year(Integer year) { this.year = year; return this; }
        public Builder periodDisplay(String periodDisplay) { this.periodDisplay = periodDisplay; return this; }
        public Builder status(TimesheetStatus status) { this.status = status; return this; }
        public Builder statusDisplay(String statusDisplay) { this.statusDisplay = statusDisplay; return this; }
        public Builder totalNormaHours(int totalNormaHours) { this.totalNormaHours = totalNormaHours; return this; }
        public Builder totalPlataOraHours(int totalPlataOraHours) { this.totalPlataOraHours = totalPlataOraHours; return this; }
        public Builder totalHours(int totalHours) { this.totalHours = totalHours; return this; }
        public Builder entries(List<TimesheetEntryResponse> entries) { this.entries = entries; return this; }
        public Builder submittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder editable(boolean editable) { this.editable = editable; return this; }
        public TimesheetResponse build() {
            return new TimesheetResponse(id, userId, userName, userEmail, departmentName, month, year, periodDisplay, status, statusDisplay, totalNormaHours, totalPlataOraHours, totalHours, entries, submittedAt, createdAt, updatedAt, editable);
        }
    }

    public static Builder builder() { return new Builder(); }

    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String departmentName;
    private Integer month;
    private Integer year;
    private String periodDisplay;
    private TimesheetStatus status;
    private String statusDisplay;
    private int totalNormaHours;
    private int totalPlataOraHours;
    private int totalHours;
    private List<TimesheetEntryResponse> entries;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean editable;

    public static TimesheetResponse fromEntity(Timesheet timesheet) {
        return fromEntity(timesheet, false);
    }

    public static TimesheetResponse fromEntity(Timesheet timesheet, boolean includeEntries) {
        Builder builder = TimesheetResponse.builder()
                .id(timesheet.getId())
                .userId(timesheet.getUser().getId())
                .userName(timesheet.getUser().getFullName())
                .userEmail(timesheet.getUser().getEmail())
                .month(timesheet.getMonth())
                .year(timesheet.getYear())
                .periodDisplay(timesheet.getPeriodDisplay())
                .status(timesheet.getStatus())
                .statusDisplay(timesheet.getStatus().getDisplayName())
                .totalNormaHours(timesheet.getTotalNormaHours())
                .totalPlataOraHours(timesheet.getTotalPlataOraHours())
                .totalHours(timesheet.getTotalHours())
                .submittedAt(timesheet.getSubmittedAt())
                .createdAt(timesheet.getCreatedAt())
                .updatedAt(timesheet.getUpdatedAt())
                .editable(timesheet.isEditable());

        if (timesheet.getUser().getDepartment() != null) {
            builder.departmentName(timesheet.getUser().getDepartment().getName());
        }

        if (includeEntries && timesheet.getEntries() != null) {
                builder.entries(timesheet.getEntries().stream()
                    .map(TimesheetEntryResponse::fromEntity)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
