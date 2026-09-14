package com.trainbooking.it25101520.service;
import com.trainbooking.it25101520.model.*;
import com.trainbooking.it25101520.dto.*;
import com.trainbooking.it25101520.repository.*;

import com.trainbooking.it25101520.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service class handling authentication operations such as registration and password reset.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Registers a new passenger user in the system.
     *
     * @param request the registration details
     */
    public void registerUser(RegisterRequest request) {
        log.info("Processing user registration for email: {}", request.getEmail());
        // TODO: Validate if user already exists, hash password with BCrypt, and persist new User entity
    }

    /**
     * Generates and dispatches a password reset link to the given user email address.
     *
     * @param email the user's registered email
     */
    public void sendPasswordResetEmail(String email) {
        log.info("Sending password reset email to: {}", email);
        // TODO: Verify email exists, create secure reset token, and send email notification
    }
}
