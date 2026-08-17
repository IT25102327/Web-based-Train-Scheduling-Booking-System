package com.trainbooking.features.payment.repository;

import com.trainbooking.features.payment.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Ticket} entity management.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Finds a ticket by its unique ticket number string.
     *
     * @param ticketNumber unique ticket number
     * @return an {@link Optional} containing the ticket if found
     */
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    /**
     * Finds a ticket issued for a specific booking ID.
     *
     * @param bookingId the booking ID
     * @return an {@link Optional} containing the ticket if found
     */
    Optional<Ticket> findByBookingId(Long bookingId);
}
