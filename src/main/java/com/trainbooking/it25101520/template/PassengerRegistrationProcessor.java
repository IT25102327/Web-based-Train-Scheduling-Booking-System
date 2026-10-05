package com.trainbooking.it25101520.template;

import com.trainbooking.it25101520.dto.RegisterRequest;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of {@link AbstractUserRegistrationTemplate} handling passenger account registrations.
 *
 * @author SLIIT Software Engineering Team (IT25101520)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PassengerRegistrationProcessor extends AbstractUserRegistrationTemplate {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void validateRequest(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email address is required for registration.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters in length.");
        }
    }

    @Override
    protected String normalizeEmail(String rawEmail) {
        return rawEmail.trim().toLowerCase();
    }

    @Override
    protected void checkDuplicateAccount(String normalizedEmail) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with email " + normalizedEmail + " already exists.");
        }
    }

    @Override
    protected User buildUserEntity(RegisterRequest request, String normalizedEmail) {
        return User.builder()
                .firstName(request.getFirstName() != null ? request.getFirstName().trim() : "")
                .lastName(request.getLastName() != null ? request.getLastName().trim() : "")
                .email(normalizedEmail)
                .role(User.Role.PASSENGER)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .notifyByEmail(true)
                .notifyBySms(true)
                .build();
    }

    @Override
    protected String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    protected User persistUser(User user) {
        User saved = userRepository.save(user);
        log.info("Persisted new passenger account ID: {} with email: {}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Override
    protected void postRegistrationHook(User savedUser) {
        log.info("Registration hook executed: Welcome audit recorded for user #{}", savedUser.getId());
    }
}
