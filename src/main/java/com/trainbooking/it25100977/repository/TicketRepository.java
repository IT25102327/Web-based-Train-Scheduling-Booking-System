package com.trainbooking.it25100977.repository;

import com.trainbooking.it25100977.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Finds all tickets issued for a specific booking ID.
     *
     * @param bookingId the booking ID
     * @return list of tickets
     */
    List<Ticket> findAllByBookingId(Long bookingId);

    /**
     * Finds a ticket by its unique ticket number string, ignoring case.
     *
     * @param ticketNumber unique ticket number
     * @return an {@link Optional} containing the ticket if found
     */
    Optional<Ticket> findByTicketNumberIgnoreCase(String ticketNumber);

    /**
     * Returns the 10 most recently issued tickets.
     *
     * @return list of recent tickets
     */
    List<Ticket> findTop10ByOrderByIdDesc();

    /**
     * Deletes tickets associated with a given booking ID.
     *
     * @param bookingId booking ID
     */
    void deleteByBookingId(Long bookingId);
}
