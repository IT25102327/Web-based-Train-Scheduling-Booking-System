package com.trainbooking.it25102327.repository;

import com.trainbooking.it25102327.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
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

    /**
     * Searches for active schedules matching origin, destination and specific day of week.
     *
     * @param origin departure station
     * @param destination destination station
     * @param dayOfWeek day of week of travel
     * @return list of matching train schedules
     */
    @Query("SELECT s FROM Schedule s JOIN FETCH s.train t JOIN FETCH s.route r " +
           "WHERE LOWER(r.origin) LIKE LOWER(CONCAT('%', :origin, '%')) " +
           "AND LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')) " +
           "AND s.dayOfWeek = :dayOfWeek " +
           "AND s.isActive = true")
    List<Schedule> findMatchingSchedules(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );

    /**
     * Searches for active schedules matching origin and destination regardless of day.
     *
     * @param origin departure station
     * @param destination destination station
     * @return list of matching train schedules
     */
    @Query("SELECT s FROM Schedule s JOIN FETCH s.train t JOIN FETCH s.route r " +
           "WHERE LOWER(r.origin) LIKE LOWER(CONCAT('%', :origin, '%')) " +
           "AND LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')) " +
           "AND s.isActive = true")
    List<Schedule> findMatchingSchedulesAnyDay(
            @Param("origin") String origin,
            @Param("destination") String destination
    );

    /**
     * Finds all schedules associated with a given train ID.
     */
    List<Schedule> findByTrainId(Long trainId);
}
