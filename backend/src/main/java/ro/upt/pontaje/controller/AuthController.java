package ro.upt.pontaje.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.upt.pontaje.service.AuthService;
import ro.upt.pontaje.service.UserService;
import ro.upt.pontaje.dto.user.UserResponse;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.dto.auth.ProfileUpdateRequest;
import ro.upt.pontaje.dto.auth.LoginRequest;
import ro.upt.pontaje.dto.auth.LoginResponse;

/**
 * Controller pentru autentificare
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Autentificare utilizator
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returnează profilul utilizatorului curent
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            // Token lipsă sau invalid -> nu întoarcem 500, ci 401
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    /**
     * Actualizează profilul utilizatorului curent
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserResponse updatedUser = userService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Logout (invalidare token pe client)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // JWT-urile sunt stateless, logout-ul se face pe client prin ștergerea token-ului
        return ResponseEntity.ok().build();
    }
}
