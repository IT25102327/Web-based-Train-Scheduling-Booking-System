package com.trainbooking.it25101520.template;

import com.trainbooking.it25101520.dto.RegisterRequest;
import com.trainbooking.it25101520.model.User;

/**
 * Abstract class defining the Template Method for user account registration workflows.
 * Implements the GoF Template Method Pattern to define the invariant sequence of registration steps
 * while allowing concrete subclasses to define role-specific entities and validation rules.
 *
 * @author SLIIT Software Engineering Team (IT25101520)
 * @version 1.0.0
 */
public abstract class AbstractUserRegistrationTemplate {

    /**
     * The Template Method governing the sequential registration algorithm.
     * Declared final to prevent alterations to the execution pipeline.
     *
     * @param request the registration request data
     * @return the successfully created and persisted {@link User} entity
     */
    public final User register(RegisterRequest request) {
        validateRequest(request);
        String normalizedEmail = normalizeEmail(request.getEmail());
        checkDuplicateAccount(normalizedEmail);
        User user = buildUserEntity(request, normalizedEmail);
        user.setPassword(encodePassword(request.getPassword()));
        User savedUser = persistUser(user);
        postRegistrationHook(savedUser);
        return savedUser;
    }

    /**
     * Validates incoming request parameters.
     *
     * @param request user registration details
     */
    protected abstract void validateRequest(RegisterRequest request);

    /**
     * Normalizes the user's email address.
     *
     * @param rawEmail user-entered email
     * @return cleaned, lower-cased email
     */
    protected abstract String normalizeEmail(String rawEmail);

    /**
     * Verifies that the normalized email does not already exist in persistent storage.
     *
     * @param normalizedEmail unique email
     */
    protected abstract void checkDuplicateAccount(String normalizedEmail);

    /**
     * Constructs the unpersisted {@link User} domain entity with role assignments.
     *
     * @param request user request data
     * @param normalizedEmail sanitized email
     * @return unpersisted User entity
     */
    protected abstract User buildUserEntity(RegisterRequest request, String normalizedEmail);

    /**
     * Hashes the plaintext password using cryptographic salt and hash algorithm.
     *
     * @param rawPassword plaintext password
     * @return salted and hashed password
     */
    protected abstract String encodePassword(String rawPassword);

    /**
     * Persists the newly constructed entity to the repository.
     *
     * @param user prepared user entity
     * @return persisted user
     */
    protected abstract User persistUser(User user);

    /**
     * Optional hook method executed after successful persistence for audit logging or dispatching welcome emails.
     *
     * @param savedUser persisted user
     */
    protected void postRegistrationHook(User savedUser) {
        // Default empty hook for subclass override
    }
}
