package com.trainbooking.it25103308.state;

import com.trainbooking.it25103308.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Concrete state representing a paid and confirmed train reservation.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@Slf4j
@Component
public class ConfirmedBookingState implements BookingState {

    @Override
    public Booking.BookingStatus getStatus() {
        return Booking.BookingStatus.CONFIRMED;
    }

    @Override
    public void confirm(Booking booking) {
        log.warn("Booking ID {} is already CONFIRMED. No status modification needed.", booking.getId());
    }

    @Override
    public void cancel(Booking booking) {
        log.info("Transitioning booking ID {} from CONFIRMED to CANCELLED", booking.getId());
        booking.setStatus(Booking.BookingStatus.CANCELLED);
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
