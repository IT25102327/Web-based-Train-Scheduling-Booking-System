package com.trainbooking.it25103308.state;

import com.trainbooking.it25103308.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Concrete state representing a temporary pending reservation with active 10-minute hold.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@Slf4j
@Component
public class PendingBookingState implements BookingState {

    @Override
    public Booking.BookingStatus getStatus() {
        return Booking.BookingStatus.PENDING;
    }

    @Override
    public void confirm(Booking booking) {
        log.info("Transitioning booking ID {} from PENDING to CONFIRMED", booking.getId());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
    }

    @Override
    public void cancel(Booking booking) {
        log.info("Transitioning booking ID {} from PENDING to CANCELLED (Seat lock released)", booking.getId());
        booking.setStatus(Booking.BookingStatus.CANCELLED);
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
