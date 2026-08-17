package com.trainbooking.features.passenger;

import com.trainbooking.features.passenger.dto.FavoriteRouteDto;
import com.trainbooking.features.passenger.dto.UserProfileDto;
import com.trainbooking.features.passenger.model.FavoriteRoute;
import com.trainbooking.features.passenger.model.User;
import com.trainbooking.features.passenger.repository.FavoriteRouteRepository;
import com.trainbooking.features.passenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service class managing passenger profiles, account details, and saved favorite routes.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerService {

    private final UserRepository userRepository;
    private final FavoriteRouteRepository favoriteRouteRepository;

    /**
     * Finds a user entity by email.
     *
     * @param email the user email address
     * @return the {@link User} entity
     */
    public User getUserByEmail(String email) {
        log.info("Fetching passenger user by email: {}", email);
        // TODO: Retrieve user from userRepository.findByEmail(email) or throw ResourceNotFoundException
        return null;
    }

    /**
     * Updates profile details of an existing passenger.
     *
     * @param userId the user ID
     * @param dto the profile update data
     * @return the updated {@link User} entity
     */
    public User updateProfile(Long userId, UserProfileDto dto) {
        log.info("Updating profile for user ID: {}", userId);
        // TODO: Load user, apply dto changes, and persist to repository
        return null;
    }

    /**
     * Retrieves all saved favorite routes for a passenger.
     *
     * @param userId the user ID
     * @return list of favorite routes
     */
    public List<FavoriteRoute> getFavoriteRoutes(Long userId) {
        log.info("Fetching favorite routes for user ID: {}", userId);
        // TODO: Retrieve list from favoriteRouteRepository.findByUserId(userId)
        return Collections.emptyList();
    }

    /**
     * Adds a new favorite route for a passenger.
     *
     * @param userId the user ID
     * @param dto the route details
     * @return the saved {@link FavoriteRoute}
     */
    public FavoriteRoute addFavoriteRoute(Long userId, FavoriteRouteDto dto) {
        log.info("Adding favorite route for user ID {}: {} -> {}", userId, dto.getOrigin(), dto.getDestination());
        // TODO: Build FavoriteRoute associated with User and save to repository
        return null;
    }

    /**
     * Removes a saved favorite route for a passenger.
     *
     * @param id the favorite route ID
     * @param userId the user ID
     */
    public void removeFavoriteRoute(Long id, Long userId) {
        log.info("Removing favorite route ID {} for user ID {}", id, userId);
        // TODO: Delete favorite route by ID and userId from favoriteRouteRepository
        favoriteRouteRepository.deleteByIdAndUserId(id, userId);
    }
}
