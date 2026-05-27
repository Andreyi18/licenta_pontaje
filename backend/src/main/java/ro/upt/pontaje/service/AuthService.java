package ro.upt.pontaje.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.pontaje.dto.auth.LoginRequest;
import ro.upt.pontaje.dto.auth.LoginResponse;
import ro.upt.pontaje.exception.ResourceNotFoundException;
import ro.upt.pontaje.exception.UnauthorizedException;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.model.UserStatus;
import ro.upt.pontaje.repository.UserRepository;
import ro.upt.pontaje.security.JwtTokenProvider;

/**
 * Serviciu pentru autentificare și autorizare
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Autentifica utilizatorul si returneaza token JWT
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Autentificare cu Spring Security
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        // Gaseste utilizatorul
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul nu a fost găsit"));

        // Verifica daca contul este activ
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Contul este dezactivat. Contactați administratorul.");
        }

        // Genereaza token JWT
        String token = jwtTokenProvider.generateToken(user);

        return LoginResponse.of(token, user);
    }

    /**
     * Valideaza token-ul JWT si returneaza utilizatorul
     */
    @Transactional(readOnly = true)
    public User validateTokenAndGetUser(String token) {
        String email = jwtTokenProvider.extractEmail(token);
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Utilizatorul nu a fost găsit"));
    }

    /**
     * Schimba parola utilizatorului
     */
    @Transactional
    public void changePassword(User user, String oldPassword, String newPassword) {
        // Verifica parola veche
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Parola veche este incorectă");
        }

        // Actualizeaza parola
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
