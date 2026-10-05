package com.trainbooking.it25102925.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete subject in the Observer Pattern managing train status subscribers and broadcasting events.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Component
public class TrainStatusEventPublisher implements TrainStatusSubject {

    private final List<TrainStatusObserver> observers = new ArrayList<>();

    public TrainStatusEventPublisher(List<TrainStatusObserver> observerList) {
        if (observerList != null) {
            observers.addAll(observerList);
            for (TrainStatusObserver o : observerList) {
                log.info("Registered TrainStatusObserver: {}", o.getObserverName());
            }
        }
    }

    @Override
    public synchronized void registerObserver(TrainStatusObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            log.info("Added TrainStatusObserver: {}", observer.getObserverName());
        }
    }

    @Override
    public synchronized void removeObserver(TrainStatusObserver observer) {
        if (observer != null) {
            observers.remove(observer);
            log.info("Removed TrainStatusObserver: {}", observer.getObserverName());
        }
    }

    @Override
    public void notifyObservers(Long trainId, String newStatus) {
        log.info("Broadcasting train status change [trainId={}, status={}] to {} observer(s)", trainId, newStatus, observers.size());
        for (TrainStatusObserver observer : observers) {
            try {
                observer.onTrainStatusChanged(trainId, newStatus);
            } catch (Exception ex) {
                log.error("Error notifying observer {}: {}", observer.getObserverName(), ex.getMessage(), ex);
            }
        }
    }
}
