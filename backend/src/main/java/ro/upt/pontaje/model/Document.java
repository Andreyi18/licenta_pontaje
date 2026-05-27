package ro.upt.pontaje.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entitatea pentru documentele generate (PDF-uri)
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_documents_user", columnList = "user_id"),
    @Index(name = "idx_documents_timesheet", columnList = "timesheet_id")
})
public class Document {

    // === Constructors ===
    public Document() {}

    public Document(UUID id, User user, Timesheet timesheet, AnnexType annexType, String filePath, String fileName, LocalDateTime generatedAt) {
        this.id = id;
        this.user = user;
        this.timesheet = timesheet;
        this.annexType = annexType;
        this.filePath = filePath;
        this.fileName = fileName;
        this.generatedAt = generatedAt;
    }

    // === Getters and Setters ===
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Timesheet getTimesheet() { return timesheet; }
    public void setTimesheet(Timesheet timesheet) { this.timesheet = timesheet; }
    public AnnexType getAnnexType() { return annexType; }
    public void setAnnexType(AnnexType annexType) { this.annexType = annexType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    // === Builder ===
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private UUID id;
        private User user;
        private Timesheet timesheet;
        private AnnexType annexType;
        private String filePath;
        private String fileName;
        private LocalDateTime generatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder timesheet(Timesheet timesheet) { this.timesheet = timesheet; return this; }
        public Builder annexType(AnnexType annexType) { this.annexType = annexType; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder generatedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; return this; }
        public Document build() {
            return new Document(id, user, timesheet, annexType, filePath, fileName, generatedAt);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private Timesheet timesheet;

    @Enumerated(EnumType.STRING)
    @Column(name = "annex_type", nullable = false, length = 10)
    private AnnexType annexType;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    // ========== Helper Methods ==========

    /**
     * Returnează extensia fișierului
     */
    public String getFileExtension() {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "pdf";
    }

    /**
     * Returnează dimensiunea afișabilă a fișierului
     */
    public String getDisplayName() {
        return annexType.getDisplayName() + " - " + 
               timesheet.getPeriodDisplay() + " - " + 
               user.getFullName();
    }
}
