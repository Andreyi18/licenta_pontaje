package ro.upt.pontaje.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request pentru actualizarea profilului utilizatorului
 */
public class ProfileUpdateRequest {

    public ProfileUpdateRequest() {}
    public ProfileUpdateRequest(String firstName, String lastName, String email, String currentPassword, String newPassword) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    @NotBlank(message = "Prenumele este obligatoriu")
    private String firstName;

    @NotBlank(message = "Numele este obligatoriu")
    private String lastName;

    @NotBlank(message = "Email-ul este obligatoriu")
    @Email(message = "Email-ul nu este valid")
    private String email;

    private String currentPassword;
    
    private String newPassword;
}
