package ro.upt.pontaje.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entitatea pentru notificari și remindere
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user", columnList = "user_id"),
    @Index(name = "idx_notifications_read", columnList = "is_read")
})
public class Notification {

    // === Constructors ===
    public Notification() {}

    public Notification(UUID id, User user, NotificationType notificationType, String subject, String message, Boolean isRead, Boolean isSent, LocalDateTime sentAt, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.notificationType = notificationType;
        this.subject = subject;
        this.message = message;
        this.isRead = isRead;
        this.isSent = isSent;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
    }

    // === Getters and Setters ===
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public Boolean getIsSent() { return isSent; }
    public void setIsSent(Boolean isSent) { this.isSent = isSent; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // === Builder ===
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private UUID id;
        private User user;
        private NotificationType notificationType;
        private String subject;
        private String message;
        private Boolean isRead = false;
        private Boolean isSent = false;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder notificationType(NotificationType notificationType) { this.notificationType = notificationType; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder isRead(Boolean isRead) { this.isRead = isRead; return this; }
        public Builder isSent(Boolean isSent) { this.isSent = isSent; return this; }
        public Builder sentAt(LocalDateTime sentAt) { this.sentAt = sentAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Notification build() {
            return new Notification(id, user, notificationType, subject, message, isRead, isSent, sentAt, createdAt);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 15)
    private NotificationType notificationType;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // methode helper

    /**
     * Marcheaza notificarea ca citita
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * Marcheaza notificarea ca trimisa prin email
     */
    public void markAsSent() {
        this.isSent = true;
        this.sentAt = LocalDateTime.now();
    }
}
