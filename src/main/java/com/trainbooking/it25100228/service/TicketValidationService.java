package com.trainbooking.it25100228.service;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for ticket QR code decoding, verification against database records, and boarding updates.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketValidationService {

    private final TicketRepository ticketRepository;
    private final com.trainbooking.it25100228.repository.BoardingLogRepository boardingLogRepository;
    private final com.trainbooking.it25100228.chain.TicketValidationChain ticketValidationChain;

    /**
     * Validates a scanned ticket QR code string, marks the ticket as boarded if valid, and returns passenger/trip details.
     * Prevents fraud by rejecting tickets already marked as boarded.
     * Uses the GoF Chain of Responsibility pattern for sequential validation rules.
     *
     * @param qrCode raw QR code string scanned by station staff or manually entered ticket number
     * @return {@link ValidationResultDto} validation outcome details
     */
    @Transactional
    public ValidationResultDto validateAndBoardTicket(String qrCode) {
        log.info("Validating ticket QR code scan via Chain of Responsibility: {}", qrCode);
        return ticketValidationChain.execute(qrCode);
    }

    /**
     * Retrieves the most recently issued tickets for testing and gate operator quick verification.
     *
     * @return list of recent tickets
     */
    @Transactional(readOnly = true)
    public List<Ticket> getRecentTickets() {
        return ticketRepository.findTop10ByOrderByIdDesc();
    }

    /**
     * Resets a ticket's boarding status to false (not boarded) so staff can re-test validation.
     *
     * @param ticketId ID of the ticket to reset
     * @return true if reset succeeded, false if not found
     */
    @Transactional
    public boolean resetBoardedStatus(Long ticketId) {
        if (ticketId == null) return false;
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        if (opt.isPresent()) {
            Ticket t = opt.get();
            t.setIsBoarded(false);
            ticketRepository.save(t);
            log.info("Reset boarding status for ticket ID {} ({}) to UNBOARDED", ticketId, t.getTicketNumber());
            return true;
        }
        return false;
    }

    /**
     * Deletes a ticket by ID, safely cleaning up any associated boarding audit logs first.
     *
     * @param ticketId ID of the ticket to delete
     * @return true if ticket was deleted, false if not found
     */
    @Transactional
    public boolean deleteTicket(Long ticketId) {
        if (ticketId == null) {
            return false;
        }
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        if (opt.isPresent()) {
            Ticket ticket = opt.get();
            log.info("Deleting ticket ID {} ({}) and cleaning up associated boarding logs", ticketId, ticket.getTicketNumber());
            List<com.trainbooking.it25100228.model.BoardingLog> logs = boardingLogRepository.findByTicketId(ticketId);
            if (logs != null && !logs.isEmpty()) {
                boardingLogRepository.deleteAll(logs);
            }
            ticketRepository.delete(ticket);
            return true;
        }
        log.warn("Cannot delete ticket ID {}: Ticket not found", ticketId);
        return false;
    }
}
