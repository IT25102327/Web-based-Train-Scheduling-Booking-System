package com.trainbooking.features.booking;

import com.trainbooking.features.booking.dto.BookingRequest;
import com.trainbooking.features.booking.model.Booking;
import com.trainbooking.features.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service class handling booking lifecycle, available seat calculations, and reservation management.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    /**
     * Calculates the number of available seats remaining for a schedule, date, and seat class.
     *
     * @param scheduleId schedule ID
     * @param date date of travel
     * @param seatClass FIRST or SECOND seat class
     * @return count of available seats
     */
    public int getAvailableSeats(Long scheduleId, LocalDate date, String seatClass) {
        log.info("Calculating available seats for schedule ID {}, date {}, class {}", scheduleId, date, seatClass);
        // TODO: Query total train capacity for class minus currently active bookings
        return 0;
    }

    /**
     * Creates a new pending train booking for a passenger.
     * NOTE: Must implement atomic seat locking / pessimistic locking to prevent race conditions during high concurrency.
     *
     * @param request booking details
     * @param userId ID of the passenger making the reservation
     * @return created {@link Booking} entity
     */
    @Transactional
    public Booking createBooking(BookingRequest request, Long userId) {
        log.info("Creating booking for user ID {} on schedule ID {}", userId, request.getScheduleId());
        // TODO: Implement atomic seat locking to prevent double booking.
        // TODO: Verify seat availability, create Booking in PENDING status, and return saved entity.
        return null;
    }

    /**
     * Retrieves booking details by ID.
     *
     * @param id booking ID
     * @return {@link Booking} entity
     */
    public Booking getBookingById(Long id) {
        log.info("Fetching booking by ID: {}", id);
        // TODO: Retrieve booking from repository or throw ResourceNotFoundException
        return bookingRepository.findById(id).orElse(null);
    }
}
