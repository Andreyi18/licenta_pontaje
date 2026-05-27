package ro.upt.pontaje.model;

import jakarta.persistence.*;
// Lombok removed
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entitatea pentru pontajul lunar
 */
@Entity
@Table(name = "timesheets", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "month", "year"}),
    indexes = {
        @Index(name = "idx_timesheets_user", columnList = "user_id"),
        @Index(name = "idx_timesheets_period", columnList = "month, year"),
        @Index(name = "idx_timesheets_status", columnList = "status")
    }
)
public class Timesheet {

    // === Constructors ===
    public Timesheet() {}

    public Timesheet(UUID id, User user, Integer month, Integer year, TimesheetStatus status, List<TimesheetEntry> entries, List<Document> documents, LocalDateTime submittedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.month = month;
        this.year = year;
        this.status = status;
        this.entries = entries;
        this.documents = documents;
        this.submittedAt = submittedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // === Getters and Setters ===
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public TimesheetStatus getStatus() { return status; }
    public void setStatus(TimesheetStatus status) { this.status = status; }
    public List<TimesheetEntry> getEntries() { return entries; }
    public void setEntries(List<TimesheetEntry> entries) { this.entries = entries; }
    public List<Document> getDocuments() { return documents; }
    public void setDocuments(List<Document> documents) { this.documents = documents; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // === Builder ===
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private UUID id;
        private User user;
        private Integer month;
        private Integer year;
        private TimesheetStatus status = TimesheetStatus.DRAFT;
        private List<TimesheetEntry> entries = new ArrayList<>();
        private List<Document> documents = new ArrayList<>();
        private LocalDateTime submittedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder month(Integer month) { this.month = month; return this; }
        public Builder year(Integer year) { this.year = year; return this; }
        public Builder status(TimesheetStatus status) { this.status = status; return this; }
        public Builder entries(List<TimesheetEntry> entries) { this.entries = entries; return this; }
        public Builder documents(List<Document> documents) { this.documents = documents; return this; }
        public Builder submittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Timesheet build() {
            return new Timesheet(id, user, month, year, status, entries, documents, submittedAt, createdAt, updatedAt);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TimesheetStatus status = TimesheetStatus.DRAFT;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetEntry> entries = new ArrayList<>();

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL)
    private List<Document> documents = new ArrayList<>();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // methode helper

    /**
     * Returneaza numarul total de ore în norma
     */
    public int getTotalNormaHours() {
        return entries.stream()
            .filter(e -> e.getHourType() == HourType.NORMA)
            .mapToInt(this::calculateEntryHours)
            .sum();
    }

    /**
     * Returneaza numărul total de ore la plata cu ora
     */
    public int getTotalPlataOraHours() {
        return entries.stream()
            .filter(e -> e.getHourType() == HourType.PLATA_ORA)
            .mapToInt(this::calculateEntryHours)
            .sum();
    }

    /**
     * Returneaza numarul total de ore
     */
    public int getTotalHours() {
        return getTotalNormaHours() + getTotalPlataOraHours();
    }

    private int calculateEntryHours(TimesheetEntry entry) {
        try {
            String[] parts = entry.getTimeSlot().split("-");
            int startHour = Integer.parseInt(parts[0].split(":")[0]);
            int endHour = Integer.parseInt(parts[1].split(":")[0]);
            return endHour - startHour;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Verifica daca pontajul poate fi editat
     */
    public boolean isEditable() {
        return status == TimesheetStatus.DRAFT;
    }

    /**
     * Marchează pontajul ca trimis
     */
    public void submit() {
        this.status = TimesheetStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * Returnează perioada în format "Luna YYYY"
     */
    public String getPeriodDisplay() {
        String[] months = {"", "Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie",
                          "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie"};
        return months[month] + " " + year;
    }
}
