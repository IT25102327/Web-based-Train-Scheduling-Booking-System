package com.trainbooking.it25102925.observer;

/**
 * Subject interface in the GoF Observer Pattern managing registered status observers.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
public interface TrainStatusSubject {

    /**
     * Registers a new train status observer.
     *
     * @param observer the observer to subscribe
     */
    void registerObserver(TrainStatusObserver observer);

    /**
     * Unregisters an existing observer.
     *
     * @param observer the observer to unsubscribe
     */
    void removeObserver(TrainStatusObserver observer);

    /**
     * Broadcasts status changes to all registered subscribers.
     *
     * @param trainId ID of the affected train
     * @param newStatus updated status
     */
    void notifyObservers(Long trainId, String newStatus);
}
