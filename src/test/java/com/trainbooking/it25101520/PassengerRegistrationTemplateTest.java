package com.trainbooking.it25101520;

import com.trainbooking.it25101520.dto.RegisterRequest;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.repository.UserRepository;
import com.trainbooking.it25101520.template.PassengerRegistrationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying the Template Method Pattern implementation for user account registration.
 *
 * @author SLIIT Software Engineering Team (IT25101520)
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PassengerRegistrationTemplateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PassengerRegistrationProcessor registrationProcessor;

    @BeforeEach
    void setUp() {
        registrationProcessor = new PassengerRegistrationProcessor(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Template Method: should enforce invariant steps and register passenger successfully")
    void testTemplateMethod_SuccessfulRegistration() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ruwan")
                .lastName("Dias")
                .email(" Ruwan.Dias@Example.COM ")
                .password("SecretPass123")
                .phone("+94711122334")
                .build();

        when(userRepository.existsByEmail("ruwan.dias@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecretPass123")).thenReturn("argon2$hashed_secret");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(42L);
            return u;
        });

        User user = registrationProcessor.register(request);

        assertNotNull(user);
        assertEquals(42L, user.getId());
        assertEquals("ruwan.dias@example.com", user.getEmail());
        assertEquals(User.Role.PASSENGER, user.getRole());
        assertEquals("argon2$hashed_secret", user.getPassword());
        verify(userRepository).existsByEmail("ruwan.dias@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Template Method: should validate input and throw exception on short password")
    void testTemplateMethod_ValidationFailure() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("123") // Less than 6 chars
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                registrationProcessor.register(request));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Template Method: should prevent duplicate email registrations")
    void testTemplateMethod_DuplicateCheck() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("SecretPass123")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                registrationProcessor.register(request));

        verify(userRepository, never()).save(any(User.class));
    }
}
