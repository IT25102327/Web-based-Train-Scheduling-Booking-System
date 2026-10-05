package com.trainbooking.it25103308.strategy;

import com.trainbooking.it25103308.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Concrete strategy validating and assigning passenger-selected custom seat numbers.
 * Verifies that none of the requested seats conflict with existing reservations.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@Slf4j
@Component
public class SelectedSeatAllocationStrategy implements SeatAllocationStrategy {

    @Override
    public boolean supports(String requestedSeatNumbers) {
        return requestedSeatNumbers != null && !requestedSeatNumbers.trim().isEmpty();
    }

    @Override
    public String allocateSeats(Booking.SeatClass seatClass, int requestedSeats, String requestedSeatNumbers, int availableSeats, List<String> bookedSeats) {
        log.info("Executing SelectedSeatAllocationStrategy for requested seats: {}", requestedSeatNumbers);
        String allocatedSeats = requestedSeatNumbers.trim();

        String[] reqParts = allocatedSeats.split(",");
        for (String seat : reqParts) {
            String cleanSeat = seat.trim();
            if (!cleanSeat.isEmpty() && isSeatInBookedList(cleanSeat, bookedSeats)) {
                throw new IllegalStateException("Seat " + cleanSeat + " is already reserved by another passenger. Please select available seats.");
            }
        }

        return allocatedSeats;
    }

    private boolean isSeatInBookedList(String fullSeatId, List<String> bookedSeats) {
        if (fullSeatId == null || bookedSeats == null || bookedSeats.isEmpty()) return false;
        String normalizedTarget = normalizeSeatId(fullSeatId);
        for (String booked : bookedSeats) {
            if (fullSeatId.equalsIgnoreCase(booked) || normalizedTarget.equalsIgnoreCase(normalizeSeatId(booked))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSeatId(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\s+", "").toLowerCase()
                .replace("coach", "car")
                .replaceAll("s0+", "s");
    }
}
