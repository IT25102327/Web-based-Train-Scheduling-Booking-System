package com.trainbooking.it25103308.strategy;

import com.trainbooking.it25103308.model.Booking;

import java.util.List;

/**
 * Strategy interface defining seat allocation algorithms for passenger reservations.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
public interface SeatAllocationStrategy {

    /**
     * Determines whether this strategy applies to the given booking request.
     *
     * @param requestedSeatNumbers passenger's explicitly selected seats, if any
     * @return true if this strategy handles the request
     */
    boolean supports(String requestedSeatNumbers);

    /**
     * Allocates seats according to the strategy algorithm.
     *
     * @param seatClass travel class (FIRST or SECOND)
     * @param requestedSeats number of passenger seats requested
     * @param requestedSeatNumbers explicit seat numbers (if applicable)
     * @param availableSeats count of seats currently available in that class
     * @param bookedSeats list of seats already booked
     * @return comma-separated allocated seat identifier string
     */
    String allocateSeats(Booking.SeatClass seatClass, int requestedSeats, String requestedSeatNumbers, int availableSeats, List<String> bookedSeats);
}
