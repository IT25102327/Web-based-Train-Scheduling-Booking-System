package com.trainbooking.it25101520.service;

import com.trainbooking.exception.ResourceNotFoundException;
import com.trainbooking.it25101520.dto.FavoriteRouteDto;
import com.trainbooking.it25101520.dto.UserProfileDto;
import com.trainbooking.it25101520.model.FavoriteRoute;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.repository.FavoriteRouteRepository;
import com.trainbooking.it25101520.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Updates profile details of an existing passenger.
     *
     * @param userId the user ID
     * @param dto the profile update data
     * @return the updated {@link User} entity
     */
    @Transactional
    public User updateProfile(Long userId, UserProfileDto dto) {
        log.info("Updating profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            user.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            user.setLastName(dto.getLastName().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().trim());
        }
        if (dto.getNotifyByEmail() != null) {
            user.setNotifyByEmail(dto.getNotifyByEmail());
        }
        if (dto.getNotifyBySms() != null) {
            user.setNotifyBySms(dto.getNotifyBySms());
        }
        if (dto.getDelayAlertThresholdMinutes() != null) {
            user.setDelayAlertThresholdMinutes(dto.getDelayAlertThresholdMinutes());
        }

        return userRepository.save(user);
    }

    /**
     * Retrieves profile details and preferences as DTO.
     *
     * @param userId the user ID
     * @return {@link UserProfileDto}
     */
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return UserProfileDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .notifyByEmail(user.getNotifyByEmail() != null ? user.getNotifyByEmail() : true)
                .notifyBySms(user.getNotifyBySms() != null ? user.getNotifyBySms() : true)
                .delayAlertThresholdMinutes(user.getDelayAlertThresholdMinutes() != null ? user.getDelayAlertThresholdMinutes() : 15)
                .build();
    }

    /**
     * Retrieves all saved favorite routes for a passenger.
     *
     * @param userId the user ID
     * @return list of favorite routes
     */
    public List<FavoriteRoute> getFavoriteRoutes(Long userId) {
        log.info("Fetching favorite routes for user ID: {}", userId);
        return favoriteRouteRepository.findByUserId(userId);
    }

    /**
     * Adds a new favorite route for a passenger.
     *
     * @param userId the user ID
     * @param dto the route details
     * @return the saved {@link FavoriteRoute}
     */
    @Transactional
    public FavoriteRoute addFavoriteRoute(Long userId, FavoriteRouteDto dto) {
        log.info("Adding favorite route for user ID {}: {} -> {}", userId, dto.getOrigin(), dto.getDestination());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        FavoriteRoute favoriteRoute = FavoriteRoute.builder()
                .user(user)
                .origin(dto.getOrigin())
                .destination(dto.getDestination())
                .build();

        return favoriteRouteRepository.save(favoriteRoute);
    }

    /**
     * Removes a saved favorite route for a passenger.
     *
     * @param id the favorite route ID
     * @param userId the user ID
     */
    @Transactional
    public void removeFavoriteRoute(Long id, Long userId) {
        log.info("Removing favorite route ID {} for user ID {}", id, userId);
        favoriteRouteRepository.deleteByIdAndUserId(id, userId);
    }
}
