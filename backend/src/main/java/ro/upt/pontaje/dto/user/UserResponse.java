package ro.upt.pontaje.dto.user;

import org.hibernate.Hibernate;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.model.UserRole;
import ro.upt.pontaje.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pentru raspunsul cu informatii utilizator
 */
public class UserResponse {

    public UserResponse() {}
    public UserResponse(UUID id, String email, String firstName, String lastName, String fullName, UserRole role, UserStatus status, UUID departmentId, String departmentName, String departmentCode, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public UUID getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getDepartmentCode() { return departmentCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static class Builder {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private UserRole role;
        private UserStatus status;
        private UUID departmentId;
        private String departmentName;
        private String departmentCode;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder status(UserStatus status) { this.status = status; return this; }
        public Builder departmentId(UUID departmentId) { this.departmentId = departmentId; return this; }
        public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
        public Builder departmentCode(String departmentCode) { this.departmentCode = departmentCode; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public UserResponse build() {
            return new UserResponse(id, email, firstName, lastName, fullName, role, status, departmentId, departmentName, departmentCode, createdAt, updatedAt);
        }
    }

    public static Builder builder() { return new Builder(); }

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private UserRole role;
    private UserStatus status;
    private UUID departmentId;
    private String departmentName;
    private String departmentCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse fromEntity(User user) {
        Builder builder = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        // Evităm LazyInitializationException când department este LAZY și nu e inițializat
        if (user.getDepartment() != null && Hibernate.isInitialized(user.getDepartment())) {
            if (user.getDepartment().getId() != null) {
                builder.departmentId(user.getDepartment().getId());
            }
            if (user.getDepartment().getName() != null) {
                builder.departmentName(user.getDepartment().getName());
            }
            if (user.getDepartment().getCode() != null) {
                builder.departmentCode(user.getDepartment().getCode());
            }
        }

        return builder.build();
    }
}
