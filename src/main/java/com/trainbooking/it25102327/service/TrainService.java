package com.trainbooking.it25102327.service;

import com.trainbooking.exception.ResourceNotFoundException;
import com.trainbooking.it25102327.dto.TrainDto;
import com.trainbooking.it25102327.dto.TrainSearchResultDto;
import com.trainbooking.it25102327.model.Route;
import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.RouteRepository;
import com.trainbooking.it25102327.repository.ScheduleRepository;
import com.trainbooking.it25102327.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Service class handling train fleet management, schedules, live status updates, and search operations.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainService {

    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;

    /**
     * Searches for available train schedules based on origin, destination, and travel date.
     *
     * @param origin departure station
     * @param destination destination station
     * @param date date of travel
     * @return list of matching {@link TrainSearchResultDto} records
     */
    public List<TrainSearchResultDto> searchTrains(String origin, String destination, LocalDate date) {
        String cleanOrigin = origin != null ? origin.trim() : "";
        String cleanDestination = destination != null ? destination.trim() : "";
        log.info("Searching trains from '{}' to '{}' on date {}", cleanOrigin, cleanDestination, date);

        if (cleanOrigin.isEmpty() && cleanDestination.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate travelDate = (date != null) ? date : LocalDate.now();
        List<Schedule> schedules = scheduleRepository.findMatchingSchedules(
                cleanOrigin,
                cleanDestination,
                travelDate.getDayOfWeek()
        );

        if (schedules.isEmpty()) {
            log.info("No day-specific schedules found, falling back to any-day schedules.");
            schedules = scheduleRepository.findMatchingSchedulesAnyDay(cleanOrigin, cleanDestination);
        }

        if (schedules.isEmpty()) {
            log.info("Query fallback: checking in-memory route matches for '{}' -> '{}'", cleanOrigin, cleanDestination);
            schedules = scheduleRepository.findAll().stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                    .filter(s -> {
                        boolean matchOrigin = cleanOrigin.isEmpty() ||
                                (s.getRoute() != null && s.getRoute().getOrigin() != null &&
                                 s.getRoute().getOrigin().toLowerCase().contains(cleanOrigin.toLowerCase()));
                        boolean matchDest = cleanDestination.isEmpty() ||
                                (s.getRoute() != null && s.getRoute().getDestination() != null &&
                                 s.getRoute().getDestination().toLowerCase().contains(cleanDestination.toLowerCase()));
                        return matchOrigin && matchDest;
                    })
                    .toList();
        }

        List<TrainSearchResultDto> results = new ArrayList<>();
        for (Schedule sch : schedules) {
            Train train = sch.getTrain();
            Route route = sch.getRoute();

            TrainSearchResultDto dto = TrainSearchResultDto.builder()
                    .scheduleId(sch.getId())
                    .trainName(train != null ? train.getTrainName() : "Express Train")
                    .trainNumber(train != null ? train.getTrainNumber() : "N/A")
                    .origin(route != null ? route.getOrigin() : cleanOrigin)
                    .destination(route != null ? route.getDestination() : cleanDestination)
                    .departureTime(sch.getDepartureTime())
                    .arrivalTime(sch.getArrivalTime())
                    .availableFirstClass(train != null ? train.getFirstClassSeats() : 0)
                    .availableSecondClass(train != null ? train.getSecondClassSeats() : 0)
                    .firstClassFare(sch.getFirstClassFare())
                    .secondClassFare(sch.getSecondClassFare())
                    .status(train != null && train.getStatus() != null ? train.getStatus().name() : "ON_TIME")
                    .build();

            results.add(dto);
        }

        log.info("Found {} matching schedules for {} -> {}", results.size(), cleanOrigin, cleanDestination);
        return results;
    }

    /**
     * Retrieves all trains in the system.
     *
     * @return list of all {@link Train} entities
     */
    public List<Train> getAllTrains() {
        log.info("Retrieving all trains");
        return trainRepository.findAll();
    }

    /**
     * Retrieves a single train by its ID.
     *
     * @param id train ID
     * @return {@link Train} entity
     */
    public Train getTrainById(Long id) {
        log.info("Fetching train by ID: {}", id);
        return trainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with ID: " + id));
    }

    /**
     * Creates and saves a new train record.
     *
     * @param dto train details
     * @return created {@link Train} entity
     */
    @Transactional
    public Train createTrain(TrainDto dto) {
        log.info("Creating new train: {}", dto.getTrainNumber());
        Train.TrainStatus status = Train.TrainStatus.ON_TIME;
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            String s = dto.getStatus().trim().toUpperCase();
            if (s.equals("ACTIVE") || s.equals("ON_TIME")) {
                status = Train.TrainStatus.ON_TIME;
            } else if (s.equals("MAINTENANCE") || s.equals("DELAYED")) {
                status = Train.TrainStatus.DELAYED;
            } else if (s.equals("INACTIVE") || s.equals("CANCELLED")) {
                status = Train.TrainStatus.CANCELLED;
            } else {
                try {
                    status = Train.TrainStatus.valueOf(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        int totalSeats = (dto.getTotalSeats() != null) ? dto.getTotalSeats() :
                ((dto.getFirstClassSeats() != null ? dto.getFirstClassSeats() : 0) +
                 (dto.getSecondClassSeats() != null ? dto.getSecondClassSeats() : 0));

        Train train = Train.builder()
                .trainNumber(dto.getTrainNumber())
                .trainName(dto.getTrainName())
                .trainType(dto.getTrainType() != null && !dto.getTrainType().isBlank() ? dto.getTrainType() : "Express Intercity")
                .totalSeats(totalSeats)
                .firstClassSeats(dto.getFirstClassSeats() != null ? dto.getFirstClassSeats() : 0)
                .secondClassSeats(dto.getSecondClassSeats() != null ? dto.getSecondClassSeats() : 0)
                .status(status)
                .build();

        return trainRepository.save(train);
    }

    /**
     * Updates an existing train record.
     *
     * @param id train ID
     * @param dto updated details
     * @return updated {@link Train} entity
     */
    @Transactional
    public Train updateTrain(Long id, TrainDto dto) {
        log.info("Updating train ID: {}", id);
        Train train = getTrainById(id);

        if (dto.getTrainNumber() != null && !dto.getTrainNumber().isBlank()) {
            train.setTrainNumber(dto.getTrainNumber());
        }
        if (dto.getTrainName() != null && !dto.getTrainName().isBlank()) {
            train.setTrainName(dto.getTrainName());
        }
        if (dto.getTrainType() != null && !dto.getTrainType().isBlank()) {
            train.setTrainType(dto.getTrainType());
        }
        if (dto.getFirstClassSeats() != null) {
            train.setFirstClassSeats(dto.getFirstClassSeats());
        }
        if (dto.getSecondClassSeats() != null) {
            train.setSecondClassSeats(dto.getSecondClassSeats());
        }
        if (dto.getTotalSeats() != null) {
            train.setTotalSeats(dto.getTotalSeats());
        } else {
            train.setTotalSeats(train.getFirstClassSeats() + train.getSecondClassSeats());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            train.setStatus(parseStatus(dto.getStatus()));
        }

        return trainRepository.save(train);
    }

    /**
     * Deletes a train by its ID.
     * Safely cascades deletion of all associated schedules, bookings, and unlinks routes
     * to prevent foreign key constraint violations.
     *
     * @param id train ID
     */
    @Transactional
    public void deleteTrain(Long id) {
        log.info("Deleting train ID with cascading schedule cleanup: {}", id);
        Train train = getTrainById(id);

        // 1. Delete all schedules operating this train
        List<Schedule> schedules = scheduleRepository.findByTrainId(id);
        for (Schedule s : schedules) {
            try {
                scheduleService.deleteSchedule(s.getId());
            } catch (Exception ex) {
                log.warn("Could not cascade delete schedule ID {} for train ID {}: {}", s.getId(), id, ex.getMessage());
            }
        }

        // 2. Unlink any route assigned to this train
        try {
            List<Route> routes = routeRepository.findAll().stream()
                    .filter(r -> r.getTrain() != null && r.getTrain().getId().equals(id))
                    .toList();
            for (Route r : routes) {
                r.setTrain(null);
                routeRepository.save(r);
            }
        } catch (Exception ex) {
            log.warn("Error unlinking routes for train ID {}: {}", id, ex.getMessage());
        }

        trainRepository.delete(train);
        log.info("Train ID {} and associated operational data deleted successfully.", id);
    }

    /**
     * Updates the operational status of a train (ON_TIME, DELAYED, CANCELLED).
     *
     * @param id train ID
     * @param status new status string
     * @return updated {@link Train} entity
     */
    @Transactional
    public Train updateTrainStatus(Long id, String status) {
        log.info("Updating status for train ID {} to {}", id, status);
        Train train = getTrainById(id);
        train.setStatus(parseStatus(status));
        return trainRepository.save(train);
    }

    private Train.TrainStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return Train.TrainStatus.ON_TIME;
        }
        String s = statusStr.trim().toUpperCase();
        if (s.equals("ACTIVE") || s.equals("ON_TIME")) {
            return Train.TrainStatus.ON_TIME;
        } else if (s.equals("MAINTENANCE") || s.equals("DELAYED")) {
            return Train.TrainStatus.DELAYED;
        } else if (s.equals("INACTIVE") || s.equals("CANCELLED")) {
            return Train.TrainStatus.CANCELLED;
        }
        try {
            return Train.TrainStatus.valueOf(s);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown train status '{}', defaulting to DELAYED", statusStr);
            return Train.TrainStatus.DELAYED;
        }
    }
}
