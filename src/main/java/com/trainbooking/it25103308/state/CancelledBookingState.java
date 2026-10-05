package com.trainbooking.it25103308.state;

import com.trainbooking.it25103308.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Concrete terminal state representing a cancelled reservation.
 * Rejects any further lifecycle transitions.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@Slf4j
@Component
public class CancelledBookingState implements BookingState {

    @Override
    public Booking.BookingStatus getStatus() {
        return Booking.BookingStatus.CANCELLED;
    }

    @Override
    public void confirm(Booking booking) {
        log.error("Invalid state transition: Cannot confirm already cancelled booking ID: {}", booking.getId());
        throw new IllegalStateException("Cannot confirm reservation: Booking has already been cancelled.");
    }

    @Override
    public void cancel(Booking booking) {
        log.warn("Booking ID {} is already CANCELLED.", booking.getId());
        throw new IllegalStateException("Booking #" + booking.getId() + " has already been cancelled.");
    }

    @Override
    public boolean canCancel() {
        return false;
    }
}
