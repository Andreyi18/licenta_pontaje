package ro.upt.pontaje.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.upt.pontaje.model.UserRole;

import java.util.UUID;

/**
 * DTO pentru crearea/actualizarea utilizatorului
 */
public class UserRequest {
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public UserRole getRole() { return role; }
    public UUID getDepartmentId() { return departmentId; }

    public UserRequest() {}
    public UserRequest(String email, String password, String firstName, String lastName, UserRole role, UUID departmentId) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.departmentId = departmentId;
    }

    @NotBlank(message = "Email-ul este obligatoriu")
    @Email(message = "Email-ul trebuie să fie valid")
    private String email;

    @Size(min = 6, message = "Parola trebuie să aibă cel puțin 6 caractere")
    private String password;

    @NotBlank(message = "Prenumele este obligatoriu")
    @Size(max = 100, message = "Prenumele nu poate depăși 100 de caractere")
    private String firstName;

    @NotBlank(message = "Numele este obligatoriu")
    @Size(max = 100, message = "Numele nu poate depăși 100 de caractere")
    private String lastName;

    @NotNull(message = "Rolul este obligatoriu")
    private UserRole role;

    private UUID departmentId;
}
