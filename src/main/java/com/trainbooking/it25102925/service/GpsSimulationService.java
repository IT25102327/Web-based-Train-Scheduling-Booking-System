package com.trainbooking.it25102925.service;

import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.TrainRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automated GPS Tracking Simulation Engine.
 * Periodically simulates train positions along geographic waypoints, detects schedule deviations,
 * and triggers passenger alerts and departure board status updates automatically.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GpsSimulationService {

    private final TrainRepository trainRepository;
    private final NotificationService notificationService;

    @Getter
    @Setter
    private boolean simulationActive = true;

    private final AtomicInteger tickCount = new AtomicInteger(0);

    /**
     * Periodic background worker simulating GPS checkpoint pings and automated deviation detection.
     * Evaluates running trains and detects realistic route delays.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void runGpsTrackingCycle() {
        if (!simulationActive) {
            log.debug("GPS Simulation is currently paused.");
            return;
        }

        int currentTick = tickCount.incrementAndGet();
        log.info("Executing GPS Simulation cycle #{} at {}", currentTick, LocalDateTime.now());

        List<Train> activeTrains = trainRepository.findAll();
        if (activeTrains.isEmpty()) return;

        // On every 3rd cycle, simulate a realistic operational delay deviation on one active train
        if (currentTick % 3 == 0) {
            Train targetTrain = activeTrains.get(currentTick % activeTrains.size());
            int delayMinutes = 15 + (currentTick % 20);
            String reason = "Signal clearance delay between Polgahawela and Rambukkana";

            log.warn("GPS ENGINE ALERT: Schedule deviation detected for train {} ({}). Deviation: +{} mins",
                    targetTrain.getTrainName(), targetTrain.getTrainNumber(), delayMinutes);

            targetTrain.setStatus(Train.TrainStatus.DELAYED);
            trainRepository.save(targetTrain);

            notificationService.notifyAffectedPassengers(
                    targetTrain.getId(),
                    "DELAYED by " + delayMinutes + " mins (" + reason + ")"
            );
        }
    }

    /**
     * Manually triggers an automated GPS milestone delay simulation for demonstration or testing.
     *
     * @param trainId train ID
     * @param delayMinutes minutes of delay
     * @param reason cause of schedule deviation
     */
    @Transactional
    public void simulateDeviation(Long trainId, int delayMinutes, String reason) {
        log.info("Simulating manual GPS delay deviation: trainId={}, delayMinutes={}, reason={}",
                trainId, delayMinutes, reason);

        Train train = trainRepository.findById(trainId).orElse(null);
        if (train != null) {
            train.setStatus(Train.TrainStatus.DELAYED);
            trainRepository.save(train);

            notificationService.notifyAffectedPassengers(
                    trainId,
                    "DELAYED by " + delayMinutes + " mins (" + reason + ")"
            );
        }
    }
}
