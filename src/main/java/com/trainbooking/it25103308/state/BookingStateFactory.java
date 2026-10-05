package com.trainbooking.it25103308.state;

import com.trainbooking.it25103308.model.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Factory class resolving the current {@link BookingState} instance for a booking entity.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class BookingStateFactory {

    private final PendingBookingState pendingState;
    private final ConfirmedBookingState confirmedState;
    private final CancelledBookingState cancelledState;

    /**
     * Resolves the corresponding state object for a booking based on its current status.
     *
     * @param booking target booking
     * @return active {@link BookingState}
     */
    public BookingState getState(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return pendingState;
        }

        return switch (booking.getStatus()) {
            case PENDING -> pendingState;
            case CONFIRMED -> confirmedState;
            case CANCELLED -> cancelledState;
        };
    }
}
