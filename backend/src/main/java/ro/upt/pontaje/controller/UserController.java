package ro.upt.pontaje.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.upt.pontaje.dto.user.UserRequest;
import ro.upt.pontaje.dto.user.UserResponse;
import ro.upt.pontaje.model.UserRole;
import ro.upt.pontaje.model.UserStatus;
import ro.upt.pontaje.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * Controller pentru gestionarea utilizatorilor (doar pentru Admin)
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returnează toți utilizatorii
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String search) {
        
        List<UserResponse> users;
        
        if (search != null && !search.isEmpty()) {
            users = userService.searchByName(search);
        } else if (role != null) {
            users = userService.findByRole(role);
        } else if (departmentId != null) {
            users = userService.findByDepartment(departmentId);
        } else {
            users = userService.findAll();
        }
        
        return ResponseEntity.ok(users);
    }

    /**
     * Returnează un utilizator după ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.fromEntity(userService.findById(id)));
    }

    /**
     * Creează un utilizator nou
     * POST /api/users
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Actualizează un utilizator
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest request) {
        UserResponse user = userService.update(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Activează sau dezactivează un utilizator
     * PATCH /api/users/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam UserStatus status) {
        UserResponse user = userService.updateStatus(id, status);
        return ResponseEntity.ok(user);
    }

    /**
     * Șterge un utilizator
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
