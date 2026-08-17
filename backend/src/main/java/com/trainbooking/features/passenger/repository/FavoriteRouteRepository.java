package com.trainbooking.features.passenger.repository;

import com.trainbooking.features.passenger.model.FavoriteRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link FavoriteRoute} entity operations.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Repository
public interface FavoriteRouteRepository extends JpaRepository<FavoriteRoute, Long> {

    /**
     * Retrieves all saved favorite routes for a given passenger.
     *
     * @param userId the user ID
     * @return list of favorite routes
     */
    List<FavoriteRoute> findByUserId(Long userId);

    /**
     * Deletes a specific favorite route belonging to a user.
     *
     * @param id the favorite route ID
     * @param userId the user ID
     */
    void deleteByIdAndUserId(Long id, Long userId);
}
