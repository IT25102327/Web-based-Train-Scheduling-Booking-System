package com.trainbooking.it25102925.service;

import com.trainbooking.it25102327.model.Route;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.RouteRepository;
import com.trainbooking.it25102327.repository.TrainRepository;
import com.trainbooking.it25102925.dto.TrainLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service managing real-time GPS coordinates, speed, and status telemetry for all active trains.
 * Integrates with external telemetry feeders (Node.js) and internal GPS simulations.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainLocationService {

    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.trainbooking.it25102925.observer.TrainStatusSubject trainStatusSubject;

    // In-memory thread-safe cache of real-time train positions
    private final ConcurrentMap<Long, TrainLocationDto> liveLocations = new ConcurrentHashMap<>();

    /**
     * Ingests a new GPS position and operational telemetry ping for a specific train.
     *
     * @param trainId train ID
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param speed speed in km/h
     * @param status operational status (optional)
     * @param currentStation station currently nearest to train (optional)
     * @param nextStation next station ahead (optional)
     * @return updated {@link TrainLocationDto} or null if train does not exist
     */
    @Transactional
    public TrainLocationDto updateLocation(
            Long trainId,
            Double latitude,
            Double longitude,
            Double speed,
            String status,
            String currentStation,
            String nextStation
    ) {
        return updateLocation(trainId, latitude, longitude, speed, status, currentStation, nextStation, null, null, null);
    }

    /**
     * Ingests a new GPS position, operational telemetry, and trip direction for a specific train.
     */
    @Transactional
    public TrainLocationDto updateLocation(
            Long trainId,
            Double latitude,
            Double longitude,
            Double speed,
            String status,
            String currentStation,
            String nextStation,
            String tripDirection,
            String customRoute,
            Integer progressPercentage
    ) {
        if (trainId == null || latitude == null || longitude == null) {
            log.warn("Invalid telemetry update: Missing required trainId ({}) or coordinates ({}, {})",
                    trainId, latitude, longitude);
            return null;
        }

        Optional<Train> trainOpt = trainRepository.findById(trainId);
        if (trainOpt.isEmpty()) {
            log.warn("Telemetry update rejected: No train exists with ID {}", trainId);
            return null;
        }

        Train train = trainOpt.get();
        double effectiveSpeed = (speed != null && speed >= 0) ? speed : 0.0;

        // Resolve route name: use custom route from feeder if provided, else resolve from DB
        String routeName = (customRoute != null && !customRoute.isBlank())
                ? customRoute.trim()
                : resolveRouteName(train);

        // Update database Train status if a new status is provided and changed
        if (status != null && !status.trim().isEmpty()) {
            try {
                Train.TrainStatus newStatus = Train.TrainStatus.valueOf(status.trim().toUpperCase());
                if (train.getStatus() != newStatus) {
                    log.info("Updating train {} ({}) status from {} to {}",
                            train.getTrainNumber(), train.getTrainName(), train.getStatus(), newStatus);
                    train.setStatus(newStatus);
                    trainRepository.save(train);

                    if (newStatus == Train.TrainStatus.DELAYED || newStatus == Train.TrainStatus.CANCELLED) {
                        if (trainStatusSubject != null) {
                            trainStatusSubject.notifyObservers(trainId, newStatus.name());
                        } else {
                            notificationService.notifyAffectedPassengers(trainId, newStatus.name());
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                log.debug("Ignored unrecognized train status string: {}", status);
            }
        }

        String effectiveStatus;
        if (status != null && status.trim().equalsIgnoreCase("AT_TERMINAL")) {
            effectiveStatus = "AT_TERMINAL";
        } else {
            effectiveStatus = train.getStatus() != null ? train.getStatus().name() : "ON_TIME";
        }
        String effectiveDirection = (tripDirection != null && !tripDirection.isBlank()) ? tripDirection.trim().toUpperCase() : "OUTBOUND";

        TrainLocationDto dto = TrainLocationDto.builder()
                .trainId(train.getId())
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .route(routeName)
                .latitude(latitude)
                .longitude(longitude)
                .speed(effectiveSpeed)
                .status(effectiveStatus)
                .currentStation(currentStation != null && !currentStation.isBlank() ? currentStation : "In Transit")
                .nextStation(nextStation != null && !nextStation.isBlank() ? nextStation : "")
                .tripDirection(effectiveDirection)
                .progressPercentage(progressPercentage != null ? Math.max(0, Math.min(100, progressPercentage)) : null)
                .updatedAt(LocalDateTime.now())
                .build();

        liveLocations.put(trainId, dto);
        log.debug("Updated live location for train {} ({}): lat={}, lng={}, speed={}, dir={}",
                train.getTrainNumber(), train.getTrainName(), latitude, longitude, effectiveSpeed, effectiveDirection);

        return dto;
    }

    /**
     * Returns the live location of a specific train.
     *
     * @param trainId train ID
     * @return Optional containing {@link TrainLocationDto} if present
     */
    public Optional<TrainLocationDto> getLocation(Long trainId) {
        if (trainId == null) return Optional.empty();
        ensureInitialized();
        return Optional.ofNullable(liveLocations.get(trainId));
    }

    /**
     * Returns the live locations of all registered trains.
     * Guarantees an entry for every active train in the database.
     *
     * @return list of {@link TrainLocationDto}
     */
    public List<TrainLocationDto> getAllLocations() {
        ensureInitialized();
        return new ArrayList<>(liveLocations.values());
    }

    /**
     * Ensures all registered trains in the database have a baseline location on the map.
     */
    private synchronized void ensureInitialized() {
        List<Train> trains = trainRepository.findAll();
        for (Train train : trains) {
            if (!liveLocations.containsKey(train.getId())) {
                String routeName = resolveRouteName(train);
                double[] defaultCoords = getDefaultCoordinatesForTrain(train);

                TrainLocationDto defaultDto = TrainLocationDto.builder()
                        .trainId(train.getId())
                        .trainNumber(train.getTrainNumber())
                        .trainName(train.getTrainName())
                        .route(routeName)
                        .latitude(defaultCoords[0])
                        .longitude(defaultCoords[1])
                        .speed(0.0)
                        .status(train.getStatus() != null ? train.getStatus().name() : "ON_TIME")
                        .currentStation("Colombo Fort (Terminal)")
                        .nextStation("Ragama")
                        .updatedAt(LocalDateTime.now())
                        .build();

                liveLocations.put(train.getId(), defaultDto);
            }
        }
    }

    private String resolveRouteName(Train train) {
        List<Route> routes = routeRepository.findAll();
        for (Route r : routes) {
            if (r.getTrain() != null && r.getTrain().getId().equals(train.getId())) {
                return r.getOrigin() + " ➔ " + r.getDestination();
            }
        }
        return "Colombo Fort ➔ Kandy";
    }

    private double[] getDefaultCoordinatesForTrain(Train train) {
        String num = train.getTrainNumber() != null ? train.getTrainNumber() : "";
        return switch (num) {
            case "1005" -> new double[]{6.9344, 79.8500}; // Podi Menike: Colombo Fort
            case "1015" -> new double[]{7.0917, 79.9997}; // Udarata Menike: Gampaha
            case "4077" -> new double[]{7.4863, 80.3623}; // Yal Devi: Kurunegala
            case "8058" -> new double[]{6.5854, 79.9607}; // Ruhunu Kumari: Kalutara
            case "1020" -> new double[]{7.2906, 80.6337}; // Denuwara Menike: Kandy
            default -> new double[]{6.9344, 79.8500};     // Default: Colombo Fort
        };
    }
}
