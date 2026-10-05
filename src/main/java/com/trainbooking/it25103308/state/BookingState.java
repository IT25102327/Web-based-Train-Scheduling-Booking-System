package com.trainbooking.it25103308.state;

import com.trainbooking.it25103308.model.Booking;

/**
 * State interface in the GoF State Pattern representing lifecycle stages of a train reservation.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
public interface BookingState {

    /**
     * Executes transition to CONFIRMED state upon payment verification.
     *
     * @param booking the target booking entity
     */
    void confirm(Booking booking);

    /**
     * Executes transition to CANCELLED state, releasing seat capacity back to inventory.
     *
     * @param booking the target booking entity
     */
    void cancel(Booking booking);

    /**
     * Checks if cancellation is permitted from the current state.
     *
     * @return true if cancellable, false otherwise
     */
    boolean canCancel();

    /**
     * Retrieves the corresponding enum status.
     *
     * @return {@link Booking.BookingStatus}
     */
    Booking.BookingStatus getStatus();
}
