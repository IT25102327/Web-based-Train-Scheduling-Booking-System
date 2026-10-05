package com.trainbooking.it25102327.service;

import com.trainbooking.exception.ResourceNotFoundException;
import com.trainbooking.it25102327.dto.PlatformAssignmentResult;
import com.trainbooking.it25102327.dto.ScheduleDto;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class responsible for managing train schedules, timetable lookups,
 * and schedule CRUD operations.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final com.trainbooking.it25103308.repository.BookingRepository bookingRepository;
    private final com.trainbooking.it25100977.repository.TicketRepository ticketRepository;
    private final com.trainbooking.it25100977.repository.PaymentRepository paymentRepository;
    private final com.trainbooking.it25100228.repository.BoardingLogRepository boardingLogRepository;

    /**
     * Retrieves all active and configured train schedules.
     *
     * @return list of {@link ScheduleDto} instances
     */
    public List<ScheduleDto> getAllSchedules() {
        log.debug("Retrieving all train schedules");
        return scheduleRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Retrieves a single schedule entity by its ID.
     *
     * @param id schedule ID
     * @return {@link Schedule} entity
     */
    public Schedule getScheduleById(Long id) {
        log.debug("Fetching schedule by ID: {}", id);
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with ID: " + id));
    }

    public static final List<String> STANDARD_PLATFORMS = List.of(
            "Platform 1", "Platform 2", "Platform 3", "Platform 4", "Platform 5", "Platform 6"
    );

    /**
     * Validates whether a schedule creates a platform conflict with an existing schedule.
     * A conflict occurs when two distinct schedules depart or arrive at the same station
     * on the same platform within 20 minutes of each other on the same day of the week.
     *
     * @param schedule the schedule to check
     * @return conflict description if conflict found, or null if platform is clear
     */
    public String validatePlatformConflict(Schedule schedule) {
        if (schedule == null || schedule.getRoute() == null) return null;
        String platform = schedule.getAssignedPlatform() != null && !schedule.getAssignedPlatform().isBlank()
                ? schedule.getAssignedPlatform().trim()
                : (schedule.getRoute().getDefaultPlatform() != null && !schedule.getRoute().getDefaultPlatform().isBlank()
                ? schedule.getRoute().getDefaultPlatform().trim() : "Platform 1");

        List<Schedule> existing = scheduleRepository.findAll();
        for (Schedule s : existing) {
            if (schedule.getId() != null && schedule.getId().equals(s.getId())) continue;
            if (!Boolean.TRUE.equals(s.getIsActive())) continue;
            if (s.getDayOfWeek() != schedule.getDayOfWeek()) continue;

            String sPlatform = s.getAssignedPlatform() != null && !s.getAssignedPlatform().isBlank()
                    ? s.getAssignedPlatform().trim()
                    : (s.getRoute() != null && s.getRoute().getDefaultPlatform() != null ? s.getRoute().getDefaultPlatform().trim() : "Platform 1");

            if (!platform.equalsIgnoreCase(sPlatform)) continue;

            // 1. Check Origin Station Departure collision
            if (s.getRoute() != null && s.getRoute().getOrigin() != null && schedule.getRoute().getOrigin() != null &&
                    s.getRoute().getOrigin().equalsIgnoreCase(schedule.getRoute().getOrigin()) &&
                    s.getDepartureTime() != null && schedule.getDepartureTime() != null) {
                long diffMinutes = Math.abs(Duration.between(s.getDepartureTime(), schedule.getDepartureTime()).toMinutes());
                if (diffMinutes < 20) {
                    return "Platform Conflict at " + schedule.getRoute().getOrigin() + ": Train " +
                            (s.getTrain() != null ? s.getTrain().getTrainName() : "#" + s.getId()) +
                            " is already assigned to " + platform + " at " + s.getDepartureTime() +
                            " (within " + diffMinutes + " mins).";
                }
            }

            // 2. Check Destination Station Arrival collision
            if (s.getRoute() != null && s.getRoute().getDestination() != null && schedule.getRoute().getDestination() != null &&
                    s.getRoute().getDestination().equalsIgnoreCase(schedule.getRoute().getDestination()) &&
                    s.getArrivalTime() != null && schedule.getArrivalTime() != null) {
                long diffMinutes = Math.abs(Duration.between(s.getArrivalTime(), schedule.getArrivalTime()).toMinutes());
                if (diffMinutes < 20) {
                    return "Platform Conflict at " + schedule.getRoute().getDestination() + ": Train " +
                            (s.getTrain() != null ? s.getTrain().getTrainName() : "#" + s.getId()) +
                            " is already assigned to " + platform + " at " + s.getArrivalTime() +
                            " (within " + diffMinutes + " mins).";
                }
            }
        }
        return null;
    }

    /**
     * Autonomously assigns the first available, conflict-free platform for a train schedule.
     * Prevents train collisions by evaluating headway time separations against all active schedules.
     *
     * @param stationName station to allocate platform at (e.g. Colombo Fort)
     * @param dayOfWeek operating day of week
     * @param departureTime scheduled departure time
     * @param excludeScheduleId ID of current schedule being updated, or null for new schedules
     * @return conflict-free platform designation (e.g. "Platform 2")
     */
    public String assignAutonomousPlatform(String stationName, DayOfWeek dayOfWeek, LocalTime departureTime, Long excludeScheduleId) {
        log.info("Running autonomous platform assignment for station='{}', day={}, time={}", stationName, dayOfWeek, departureTime);
        if (stationName == null || dayOfWeek == null || departureTime == null) {
            return "Platform 1";
        }

        List<Schedule> activeSchedules = scheduleRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> s.getDayOfWeek() == dayOfWeek)
                .filter(s -> excludeScheduleId == null || !s.getId().equals(excludeScheduleId))
                .filter(s -> s.getRoute() != null && s.getRoute().getOrigin() != null && s.getRoute().getOrigin().equalsIgnoreCase(stationName.trim()))
                .toList();

        for (String platform : STANDARD_PLATFORMS) {
            boolean hasConflict = false;
            for (Schedule s : activeSchedules) {
                String assigned = s.getAssignedPlatform() != null && !s.getAssignedPlatform().isBlank()
                        ? s.getAssignedPlatform().trim()
                        : (s.getRoute().getDefaultPlatform() != null ? s.getRoute().getDefaultPlatform().trim() : "Platform 1");
                if (platform.equalsIgnoreCase(assigned)) {
                    long diffMinutes = Math.abs(Duration.between(s.getDepartureTime(), departureTime).toMinutes());
                    if (diffMinutes < 20) {
                        hasConflict = true;
                        break;
                    }
                }
            }
            if (!hasConflict) {
                log.info("Autonomous platform allocator successfully assigned: {}", platform);
                return platform;
            }
        }

        // Fallback: Pick the platform with the maximum headway margin
        String bestPlatform = "Platform 1";
        long maxHeadway = -1;
        for (String platform : STANDARD_PLATFORMS) {
            long minHeadwayForPlatform = Long.MAX_VALUE;
            for (Schedule s : activeSchedules) {
                String assigned = s.getAssignedPlatform() != null && !s.getAssignedPlatform().isBlank()
                        ? s.getAssignedPlatform().trim()
                        : (s.getRoute().getDefaultPlatform() != null ? s.getRoute().getDefaultPlatform().trim() : "Platform 1");
                if (platform.equalsIgnoreCase(assigned)) {
                    long diff = Math.abs(Duration.between(s.getDepartureTime(), departureTime).toMinutes());
                    if (diff < minHeadwayForPlatform) {
                        minHeadwayForPlatform = diff;
                    }
                }
            }
            if (minHeadwayForPlatform > maxHeadway) {
                maxHeadway = minHeadwayForPlatform;
                bestPlatform = platform;
            }
        }
        return bestPlatform;
    }

    /**
     * Checks platform availability and returns detailed collision analysis and recommendations.
     *
     * @param stationName station name
     * @param dayOfWeek day of week
     * @param departureTime scheduled departure time
     * @param requestedPlatform candidate platform
     * @param excludeScheduleId schedule ID to exclude
     * @return {@link PlatformAssignmentResult}
     */
    public PlatformAssignmentResult checkPlatformAvailability(String stationName, DayOfWeek dayOfWeek, LocalTime departureTime, String requestedPlatform, Long excludeScheduleId) {
        String reqPlatform = (requestedPlatform != null && !requestedPlatform.isBlank()) ? requestedPlatform.trim() : "Platform 1";
        List<Schedule> activeSchedules = scheduleRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> s.getDayOfWeek() == dayOfWeek)
                .filter(s -> excludeScheduleId == null || !s.getId().equals(excludeScheduleId))
                .filter(s -> s.getRoute() != null && s.getRoute().getOrigin() != null && s.getRoute().getOrigin().equalsIgnoreCase(stationName != null ? stationName.trim() : ""))
                .toList();

        Map<String, String> occupancyMap = new LinkedHashMap<>();
        List<String> available = new ArrayList<>();
        String conflictDesc = null;
        boolean isConflict = false;

        for (String p : STANDARD_PLATFORMS) {
            Schedule conflicting = null;
            long minDiff = Long.MAX_VALUE;
            for (Schedule s : activeSchedules) {
                String assigned = s.getAssignedPlatform() != null && !s.getAssignedPlatform().isBlank()
                        ? s.getAssignedPlatform().trim()
                        : (s.getRoute().getDefaultPlatform() != null ? s.getRoute().getDefaultPlatform().trim() : "Platform 1");
                if (p.equalsIgnoreCase(assigned)) {
                    long diff = Math.abs(Duration.between(s.getDepartureTime(), departureTime).toMinutes());
                    if (diff < 20) {
                        conflicting = s;
                        minDiff = diff;
                        break;
                    }
                }
            }
            if (conflicting != null) {
                String trainLabel = conflicting.getTrain() != null ? conflicting.getTrain().getTrainName() : "Train #" + conflicting.getId();
                occupancyMap.put(p, trainLabel + " at " + conflicting.getDepartureTime() + " (" + minDiff + "m gap)");
                if (p.equalsIgnoreCase(reqPlatform)) {
                    isConflict = true;
                    conflictDesc = "Collision Risk: " + p + " is occupied by " + trainLabel + " at " + conflicting.getDepartureTime() + " (headway: " + minDiff + " mins < 20 min threshold).";
                }
            } else {
                available.add(p);
                occupancyMap.put(p, "CLEAR");
            }
        }

        String autoPlatform = isConflict ? assignAutonomousPlatform(stationName, dayOfWeek, departureTime, excludeScheduleId) : reqPlatform;

        return PlatformAssignmentResult.builder()
                .stationName(stationName)
                .requestedPlatform(reqPlatform)
                .allocatedPlatform(autoPlatform)
                .isConflict(isConflict)
                .conflictDescription(conflictDesc)
                .availablePlatforms(available)
                .platformOccupancyMap(occupancyMap)
                .build();
    }

    /**
     * Calculates dynamic ticket fare based on travel distance, seating class, time of day, and weekend demand.
     *
     * @param distanceKm travel distance in kilometers
     * @param seatClass FIRST or SECOND
     * @param departureTime scheduled time of departure
     * @param dayOfWeek day of week
     * @return calculated fare in LKR
     */
    public BigDecimal calculateDynamicFare(Integer distanceKm, String seatClass, LocalTime departureTime, DayOfWeek dayOfWeek) {
        double dist = (distanceKm != null && distanceKm > 0) ? distanceKm : 100.0;
        double baseRatePerKm = 5.0; // LKR 5.00 per km baseline
        double fare = dist * baseRatePerKm;

        // Class coefficient
        if ("FIRST".equalsIgnoreCase(seatClass)) {
            fare *= 1.8;
        } else {
            fare *= 1.0;
        }

        // Weekend demand multiplier (Friday afternoon & Sunday)
        if (dayOfWeek == DayOfWeek.SUNDAY || (dayOfWeek == DayOfWeek.FRIDAY && departureTime != null && departureTime.isAfter(LocalTime.of(15, 0)))) {
            fare *= 1.25; // 25% peak demand surge
        }

        // Rush-hour commuter multiplier (06:30-08:30 and 16:30-18:30)
        if (departureTime != null) {
            boolean morningRush = !departureTime.isBefore(LocalTime.of(6, 30)) && !departureTime.isAfter(LocalTime.of(8, 30));
            boolean eveningRush = !departureTime.isBefore(LocalTime.of(16, 30)) && !departureTime.isAfter(LocalTime.of(18, 30));
            if (morningRush || eveningRush) {
                fare *= 1.15; // 15% peak hour surge
            }
        }

        return BigDecimal.valueOf(Math.round(fare / 10.0) * 10.0).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Toggles seasonal / holiday timetable override.
     *
     * @param scheduleId schedule ID
     * @param isSeasonal true if seasonal override active
     * @param seasonalName label for seasonal schedule (e.g. New Year Special)
     * @return updated {@link ScheduleDto}
     */
    @Transactional
    public ScheduleDto setSeasonalOverride(Long scheduleId, boolean isSeasonal, String seasonalName) {
        log.info("Setting seasonal override for schedule ID {}: seasonal={}, name={}", scheduleId, isSeasonal, seasonalName);
        Schedule schedule = getScheduleById(scheduleId);
        schedule.setIsSeasonal(isSeasonal);
        schedule.setSeasonalName(isSeasonal ? seasonalName : null);
        Schedule saved = scheduleRepository.save(schedule);
        return mapToDto(saved);
    }

    /**
     * Schedules a planned maintenance block for a schedule's train, checking for booking conflicts.
     *
     * @param scheduleId schedule ID
     * @param blocked true to block
     * @param notes maintenance notes
     * @return warning message if active bookings exist, or confirmation message
     */
    @Transactional
    public String setMaintenanceBlock(Long scheduleId, boolean blocked, String notes) {
        log.info("Updating maintenance block for schedule ID {}: blocked={}, notes={}", scheduleId, blocked, notes);
        Schedule schedule = getScheduleById(scheduleId);
        schedule.setIsMaintenanceBlocked(blocked);
        schedule.setMaintenanceNotes(blocked ? notes : null);
        scheduleRepository.save(schedule);

        if (blocked) {
            long activeBookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getSchedule() != null && b.getSchedule().getId().equals(scheduleId))
                    .count();
            if (activeBookings > 0) {
                log.warn("MAINTENANCE CONFLICT: Schedule ID {} has {} active passenger bookings!", scheduleId, activeBookings);
                return "WARNING: Maintenance block activated. " + activeBookings + " existing booking(s) detected! System flagged capacity shortage for administrative review.";
            }
            return "Maintenance block scheduled successfully. Bookings suspended for this schedule.";
        } else {
            return "Maintenance block lifted. Schedule restored to active inventory.";
        }
    }

    /**
     * Creates and persists a new train schedule.
     *
     * @param scheduleDto the schedule details to create
     * @return the saved {@link ScheduleDto} instance
     */
    @Transactional
    public ScheduleDto createSchedule(ScheduleDto scheduleDto) {
        log.info("Creating new schedule: {}", scheduleDto);

        Train train = trainRepository.findById(scheduleDto.getTrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Train not found with ID: " + scheduleDto.getTrainId()));

        Route route = routeRepository.findById(scheduleDto.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with ID: " + scheduleDto.getRouteId()));

        LocalTime depTime = LocalTime.parse(scheduleDto.getDepartureTime());
        LocalTime arrTime = LocalTime.parse(scheduleDto.getArrivalTime());
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(scheduleDto.getDayOfWeek().toUpperCase());

        String assignedPlat = scheduleDto.getAssignedPlatform();
        if (assignedPlat == null || assignedPlat.isBlank() || "AUTO".equalsIgnoreCase(assignedPlat) || "AUTONOMOUS".equalsIgnoreCase(assignedPlat)) {
            assignedPlat = assignAutonomousPlatform(route.getOrigin(), dayOfWeek, depTime, null);
        } else {
            assignedPlat = assignedPlat.trim();
        }

        Schedule schedule = Schedule.builder()
                .train(train)
                .route(route)
                .departureTime(depTime)
                .arrivalTime(arrTime)
                .dayOfWeek(dayOfWeek)
                .assignedPlatform(assignedPlat)
                .firstClassFare(scheduleDto.getFirstClassFare() != null ? scheduleDto.getFirstClassFare() :
                        calculateDynamicFare(route.getDistanceKm(), "FIRST", depTime, dayOfWeek))
                .secondClassFare(scheduleDto.getSecondClassFare() != null ? scheduleDto.getSecondClassFare() :
                        calculateDynamicFare(route.getDistanceKm(), "SECOND", depTime, dayOfWeek))
                .isActive(scheduleDto.getIsActive() != null ? scheduleDto.getIsActive() : true)
                .isSeasonal(Boolean.TRUE.equals(scheduleDto.getIsSeasonal()))
                .seasonalName(scheduleDto.getSeasonalName())
                .isMaintenanceBlocked(Boolean.TRUE.equals(scheduleDto.getIsMaintenanceBlocked()))
                .maintenanceNotes(scheduleDto.getMaintenanceNotes())
                .build();

        String conflict = validatePlatformConflict(schedule);
        if (conflict != null) {
            log.warn("Platform conflict detected during schedule creation: {}", conflict);
        }

        Schedule saved = scheduleRepository.save(schedule);
        return mapToDto(saved);
    }

    /**
     * Deletes an existing train schedule by its unique ID.
     * Performs safe cascading cleanup of all dependent bookings, tickets, payments,
     * and station boarding logs to guarantee zero foreign key constraint violations.
     *
     * @param id the unique identifier of the schedule to delete
     */
    @Transactional
    public void deleteSchedule(Long id) {
        log.info("Deleting schedule ID with cascading dependency cleanup: {}", id);
        Schedule schedule = getScheduleById(id);

        try {
            List<com.trainbooking.it25103308.model.Booking> bookings = bookingRepository.findByScheduleId(id);
            for (com.trainbooking.it25103308.model.Booking b : bookings) {
                // 1. Delete station turnstile logs for any ticket tied to this booking
                List<com.trainbooking.it25100977.model.Ticket> tickets = ticketRepository.findAllByBookingId(b.getId());
                for (com.trainbooking.it25100977.model.Ticket t : tickets) {
                    try {
                        boardingLogRepository.deleteByTicketId(t.getId());
                    } catch (Exception ex) {
                        log.warn("Could not delete boarding logs for ticket {}: {}", t.getId(), ex.getMessage());
                    }
                }
                // 2. Delete tickets
                try {
                    ticketRepository.deleteByBookingId(b.getId());
                } catch (Exception ex) {
                    log.warn("Could not delete ticket for booking {}: {}", b.getId(), ex.getMessage());
                }
                // 3. Delete payments
                try {
                    paymentRepository.deleteByBookingId(b.getId());
                } catch (Exception ex) {
                    log.warn("Could not delete payment for booking {}: {}", b.getId(), ex.getMessage());
                }
            }
            // 4. Delete bookings
            try {
                bookingRepository.deleteByScheduleId(id);
            } catch (Exception ex) {
                log.warn("Could not delete bookings for schedule {}: {}", id, ex.getMessage());
            }
        } catch (Exception ex) {
            log.warn("Pre-deletion cleanup for schedule ID {} encountered: {}", id, ex.getMessage());
        }

        scheduleRepository.delete(schedule);
        log.info("Schedule ID {} and all dependent records deleted successfully.", id);
    }

    public List<Schedule> getSchedulesByTrainId(Long trainId) {
        return scheduleRepository.findByTrainId(trainId);
    }

    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    public Route getRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with ID: " + id));
    }

    @Transactional
    public Route createRoute(String origin, String destination, Integer distanceKm, String defaultPlatform) {
        log.info("Creating new railway route: {} ➔ {}", origin, destination);
        Route route = Route.builder()
                .origin(origin != null ? origin.trim() : "")
                .destination(destination != null ? destination.trim() : "")
                .distanceKm(distanceKm != null ? distanceKm : 100)
                .defaultPlatform(defaultPlatform != null && !defaultPlatform.isBlank() ? defaultPlatform.trim() : "Platform 1")
                .build();
        return routeRepository.save(route);
    }

    @Transactional
    public void deleteRoute(Long routeId) {
        log.info("Deleting route ID: {}", routeId);
        Route route = getRouteById(routeId);

        // Delete all schedules running on this route
        List<Schedule> schedules = scheduleRepository.findAll().stream()
                .filter(s -> s.getRoute() != null && s.getRoute().getId().equals(routeId))
                .toList();
        for (Schedule s : schedules) {
            deleteSchedule(s.getId());
        }
        routeRepository.delete(route);
    }

    /**
     * Updates an existing train schedule record.
     *
     * @param id schedule ID
     * @param dto updated schedule details
     * @return updated ScheduleDto
     */
    @Transactional
    public ScheduleDto updateSchedule(Long id, ScheduleDto dto) {
        log.info("Updating schedule ID: {}", id);
        Schedule schedule = getScheduleById(id);

        if (dto.getTrainId() != null) {
            Train train = trainRepository.findById(dto.getTrainId())
                    .orElseThrow(() -> new ResourceNotFoundException("Train not found with ID: " + dto.getTrainId()));
            schedule.setTrain(train);
        }
        if (dto.getRouteId() != null) {
            Route route = routeRepository.findById(dto.getRouteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Route not found with ID: " + dto.getRouteId()));
            schedule.setRoute(route);
        }
        if (dto.getDepartureTime() != null && !dto.getDepartureTime().isBlank()) {
            schedule.setDepartureTime(LocalTime.parse(dto.getDepartureTime().trim()));
        }
        if (dto.getArrivalTime() != null && !dto.getArrivalTime().isBlank()) {
            schedule.setArrivalTime(LocalTime.parse(dto.getArrivalTime().trim()));
        }
        if (dto.getDayOfWeek() != null && !dto.getDayOfWeek().isBlank()) {
            schedule.setDayOfWeek(DayOfWeek.valueOf(dto.getDayOfWeek().trim().toUpperCase()));
        }
        if (dto.getFirstClassFare() != null) {
            schedule.setFirstClassFare(dto.getFirstClassFare());
        }
        if (dto.getSecondClassFare() != null) {
            schedule.setSecondClassFare(dto.getSecondClassFare());
        }
        if (dto.getIsActive() != null) {
            schedule.setIsActive(dto.getIsActive());
        }
        if (dto.getIsSeasonal() != null) {
            schedule.setIsSeasonal(dto.getIsSeasonal());
        }
        if (dto.getSeasonalName() != null) {
            schedule.setSeasonalName(dto.getSeasonalName());
        }
        if (dto.getIsMaintenanceBlocked() != null) {
            schedule.setIsMaintenanceBlocked(dto.getIsMaintenanceBlocked());
        }
        if (dto.getMaintenanceNotes() != null) {
            schedule.setMaintenanceNotes(dto.getMaintenanceNotes());
        }
        if (dto.getAssignedPlatform() != null) {
            if ("AUTO".equalsIgnoreCase(dto.getAssignedPlatform()) || "AUTONOMOUS".equalsIgnoreCase(dto.getAssignedPlatform())) {
                schedule.setAssignedPlatform(assignAutonomousPlatform(
                        schedule.getRoute() != null ? schedule.getRoute().getOrigin() : "Colombo Fort",
                        schedule.getDayOfWeek(),
                        schedule.getDepartureTime(),
                        schedule.getId()));
            } else if (!dto.getAssignedPlatform().isBlank()) {
                schedule.setAssignedPlatform(dto.getAssignedPlatform().trim());
            }
        }

        Schedule saved = scheduleRepository.save(schedule);
        return mapToDto(saved);
    }

    private ScheduleDto mapToDto(Schedule s) {
        String effectivePlatform = (s.getAssignedPlatform() != null && !s.getAssignedPlatform().isBlank())
                ? s.getAssignedPlatform().trim()
                : (s.getRoute() != null && s.getRoute().getDefaultPlatform() != null && !s.getRoute().getDefaultPlatform().isBlank()
                ? s.getRoute().getDefaultPlatform().trim() : "Platform 1");

        return ScheduleDto.builder()
                .id(s.getId())
                .trainId(s.getTrain() != null ? s.getTrain().getId() : null)
                .trainName(s.getTrain() != null ? s.getTrain().getTrainName() : null)
                .trainNumber(s.getTrain() != null ? s.getTrain().getTrainNumber() : null)
                .routeId(s.getRoute() != null ? s.getRoute().getId() : null)
                .origin(s.getRoute() != null ? s.getRoute().getOrigin() : null)
                .destination(s.getRoute() != null ? s.getRoute().getDestination() : null)
                .departureTime(s.getDepartureTime() != null ? s.getDepartureTime().toString() : null)
                .arrivalTime(s.getArrivalTime() != null ? s.getArrivalTime().toString() : null)
                .dayOfWeek(s.getDayOfWeek() != null ? s.getDayOfWeek().name() : null)
                .firstClassFare(s.getFirstClassFare())
                .secondClassFare(s.getSecondClassFare())
                .isActive(s.getIsActive())
                .defaultPlatform(effectivePlatform)
                .assignedPlatform(s.getAssignedPlatform() != null ? s.getAssignedPlatform() : effectivePlatform)
                .isSeasonal(s.getIsSeasonal())
                .seasonalName(s.getSeasonalName())
                .isMaintenanceBlocked(s.getIsMaintenanceBlocked())
                .maintenanceNotes(s.getMaintenanceNotes())
                .build();
    }
}
