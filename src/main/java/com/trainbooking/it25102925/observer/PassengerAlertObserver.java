package com.trainbooking.it25102925.observer;

import com.trainbooking.it25102925.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Concrete observer that triggers automated passenger notifications when train delays or cancellations occur.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Component
public class PassengerAlertObserver implements TrainStatusObserver {

    private final NotificationService notificationService;

    public PassengerAlertObserver(@Lazy NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public String getObserverName() {
        return "PASSENGER_ALERT_OBSERVER";
    }

    @Override
    public void onTrainStatusChanged(Long trainId, String newStatus) {
        log.info("PassengerAlertObserver received train status change event for train ID: {}, newStatus: {}", trainId, newStatus);
        if (notificationService != null) {
            notificationService.notifyAffectedPassengers(trainId, newStatus);
        }
    }
}
