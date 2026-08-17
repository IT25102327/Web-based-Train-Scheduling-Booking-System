package com.trainbooking.features.trains.repository;

import com.trainbooking.features.trains.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Schedule} entity management.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * Finds active schedules for routes between the specified origin and destination.
     *
     * @param origin departure station
     * @param destination destination station
     * @param isActive schedule active status
     * @return list of matching train schedules
     */
    List<Schedule> findByRouteOriginAndRouteDestinationAndIsActive(String origin, String destination, Boolean isActive);
}
