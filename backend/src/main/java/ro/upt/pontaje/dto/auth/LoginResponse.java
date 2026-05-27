package ro.upt.pontaje.dto.auth;

import ro.upt.pontaje.model.UserRole;

import java.util.UUID;

/**
 * DTO pentru răspunsul de login (include token JWT și informații utilizator)
 */
public class LoginResponse {

    public LoginResponse() {}

    public LoginResponse(String token, String tokenType, UUID userId, String email, String firstName, String lastName, UserRole role, String departmentName, UUID departmentId) {
        this.token = token;
        this.tokenType = tokenType;
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.departmentName = departmentName;
        this.departmentId = departmentId;
    }

    // fields defined above, removed duplicates

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String token;
        private String tokenType;
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private UserRole role;
        private String departmentName;
        private UUID departmentId;

        public Builder token(String token) { this.token = token; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder role(UserRole role) { this.role = role; return this; }
        public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
        public Builder departmentId(UUID departmentId) { this.departmentId = departmentId; return this; }
        public LoginResponse build() {
            return new LoginResponse(token, tokenType, userId, email, firstName, lastName, role, departmentName, departmentId);
        }
    }

    private String token;
    private String tokenType;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String departmentName;
    private UUID departmentId;

    public static LoginResponse of(String token, ro.upt.pontaje.model.User user) {
        Builder builder = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole());

        if (user.getDepartment() != null) {
            builder.departmentName(user.getDepartment().getName())
                   .departmentId(user.getDepartment().getId());
        }

        return builder.build();
    }
}
