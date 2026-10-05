package com.trainbooking.it25103308.strategy;

import com.trainbooking.it25103308.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Concrete strategy automatically allocating consecutive coach seats for reservations
 * where the passenger did not specify custom seat numbers.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@Slf4j
@Component
public class AutoSequentialSeatAllocationStrategy implements SeatAllocationStrategy {

    @Override
    public boolean supports(String requestedSeatNumbers) {
        return requestedSeatNumbers == null || requestedSeatNumbers.trim().isEmpty();
    }

    @Override
    public String allocateSeats(Booking.SeatClass seatClass, int requestedSeats, String requestedSeatNumbers, int availableSeats, List<String> bookedSeats) {
        log.info("Executing AutoSequentialSeatAllocationStrategy for {} {} seat(s)", requestedSeats, seatClass);
        String coach = (seatClass == Booking.SeatClass.FIRST) ? "Car 01" : "Car 02";
        int seatStartNum = (availableSeats % 50) + 1;
        return coach + " / S" + seatStartNum + (requestedSeats > 1 ? (" - S" + (seatStartNum + requestedSeats - 1)) : "");
    }
}
