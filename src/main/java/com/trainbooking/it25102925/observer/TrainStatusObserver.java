package com.trainbooking.it25102925.observer;

/**
 * Observer interface in the GoF Observer Pattern for listening to real-time train status changes.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
public interface TrainStatusObserver {

    /**
     * Callback triggered when a train operational status changes (e.g. DELAYED, CANCELLED).
     *
     * @param trainId ID of the affected train
     * @param newStatus updated status description
     */
    void onTrainStatusChanged(Long trainId, String newStatus);

    /**
     * Unique identifier for the observer.
     *
     * @return observer name
     */
    String getObserverName();
}
