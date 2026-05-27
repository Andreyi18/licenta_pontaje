package ro.upt.pontaje.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.pontaje.dto.user.UserRequest;
import ro.upt.pontaje.dto.user.UserResponse;
import ro.upt.pontaje.exception.BadRequestException;
import ro.upt.pontaje.exception.ResourceNotFoundException;
import ro.upt.pontaje.model.Department;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.model.UserRole;
import ro.upt.pontaje.model.UserStatus;
import ro.upt.pontaje.repository.DepartmentRepository;
import ro.upt.pontaje.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviciu pentru gestionarea utilizatorilor
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Găsește un utilizator după ID
     */
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul nu a fost găsit"));
    }

    /**
     * Găsește un utilizator după email
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul nu a fost găsit"));
    }

    /**
     * Returnează toți utilizatorii
     */
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
            .map(UserResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Returnează utilizatorii după rol
     */
    @Transactional(readOnly = true)
    public List<UserResponse> findByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
            .map(UserResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Returnează utilizatorii dintr-un departament
     */
    @Transactional(readOnly = true)
    public List<UserResponse> findByDepartment(UUID departmentId) {
        return userRepository.findByDepartmentIdAndStatus(departmentId, UserStatus.ACTIVE).stream()
            .map(UserResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Caută utilizatori după nume
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchByName(String name) {
        return userRepository.searchByName(name).stream()
            .map(UserResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Creează un utilizator nou
     */
    @Transactional
    public UserResponse create(UserRequest request) {
        // Verifică dacă email-ul există deja
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email-ul este deja folosit");
        }

        // Validează parola
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BadRequestException("Parola trebuie să aibă cel puțin 6 caractere");
        }

        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(request.getRole())
            .status(UserStatus.ACTIVE)
            .build();

        // Setează departamentul dacă este specificat
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Departamentul nu a fost găsit"));
            user.setDepartment(department);
        }

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Actualizează un utilizator
     */
    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        User user = findById(id);

        // Verifică dacă email-ul nou nu este deja folosit de altcineva
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email-ul este deja folosit");
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());

        // Actualizează parola doar dacă este furnizată
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (request.getPassword().length() < 6) {
                throw new BadRequestException("Parola trebuie să aibă cel puțin 6 caractere");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Actualizează departamentul
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Departamentul nu a fost găsit"));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Activează sau dezactivează un utilizator
     */
    @Transactional
    public UserResponse updateStatus(UUID id, UserStatus status) {
        User user = findById(id);
        user.setStatus(status);
        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Șterge un utilizator
     */
    @Transactional
    public void delete(UUID id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    /**
     * Actualizează profilul utilizatorului curent
     */
    @Transactional
    public UserResponse updateProfile(UUID id, ro.upt.pontaje.dto.auth.ProfileUpdateRequest request) {
        User user = findById(id);

        // Verifică dacă email-ul nou nu este deja folosit de altcineva
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email-ul este deja folosit");
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Actualizează parola dacă este furnizată
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            // Verifică parola curentă
            if (request.getCurrentPassword() == null || 
                !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Parola curentă este incorectă");
            }
            
            if (request.getNewPassword().length() < 6) {
                throw new BadRequestException("Noua parolă trebuie să aibă cel puțin 6 caractere");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }
}
