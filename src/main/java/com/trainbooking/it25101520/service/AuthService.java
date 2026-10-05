package com.trainbooking.it25101520.service;
import com.trainbooking.it25101520.dto.RegisterRequest;
import com.trainbooking.it25101520.model.*;
import com.trainbooking.it25101520.repository.*;
import com.trainbooking.it25101520.template.PassengerRegistrationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service class handling authentication operations such as registration and password reset.
 *
 * @author SLIIT Software Engineering Team (IT25101520)
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PassengerRegistrationProcessor registrationProcessor;

    private PassengerRegistrationProcessor getRegistrationProcessor() {
        if (this.registrationProcessor == null) {
            this.registrationProcessor = new PassengerRegistrationProcessor(userRepository, passwordEncoder);
        }
        return this.registrationProcessor;
    }

    /**
     * Registers a new passenger user in the system using the Template Method Pattern.
     *
     * @param request the registration details
     * @return the newly registered and persisted {@link User} entity
     */
    public User registerUser(RegisterRequest request) {
        log.info("Processing user registration for email: {}", request.getEmail());
        return getRegistrationProcessor().register(request);
    }

    /**
     * Generates a secure password reset token and saves it in the database.
     *
     * @param email the user's registered email
     * @return generated reset token string if user exists, or null
     */
    public String createPasswordResetToken(String email) {
        log.info("Creating password reset token for: {}", email);
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", normalizedEmail);
            return null;
        }

        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(30))
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("Generated reset token: {} for user: {}", token, user.getEmail());
        return token;
    }

    /**
     * Generates and dispatches a password reset link to the given user email address.
     *
     * @param email the user's registered email
     */
    public void sendPasswordResetEmail(String email) {
        String token = createPasswordResetToken(email);
        if (token != null) {
            log.info("Password reset link ready: /reset-password?token={}", token);
        }
    }

    /**
     * Validates whether a password reset token exists and has not expired.
     *
     * @param token reset token string
     * @return true if valid and active
     */
    public boolean validatePasswordResetToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return passwordResetTokenRepository.findByToken(token)
                .map(t -> !t.isExpired())
                .orElse(false);
    }

    /**
     * Resets a user's password given a valid token.
     *
     * @param token reset token string
     * @param newPassword new unencoded password
     * @return true if password was successfully updated
     */
    public boolean resetPassword(String token, String newPassword) {
        log.info("Attempting password reset with token: {}", token);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token).orElse(null);
        if (resetToken == null || resetToken.isExpired()) {
            log.warn("Invalid or expired password reset token: {}", token);
            return false;
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Remove token once consumed
        passwordResetTokenRepository.delete(resetToken);
        log.info("Password reset successful for user: {}", user.getEmail());
        return true;
    }
}
