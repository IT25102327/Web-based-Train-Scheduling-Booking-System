package com.trainbooking.it25100228.service;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.repository.TicketRepository;
import com.trainbooking.it25103308.model.Booking;
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

    /**
     * Validates a scanned ticket QR code string, marks the ticket as boarded if valid, and returns passenger/trip details.
     * Prevents fraud by rejecting tickets already marked as boarded.
     *
     * @param qrCode raw QR code string scanned by station staff or manually entered ticket number
     * @return {@link ValidationResultDto} validation outcome details
     */
    @Transactional
    public ValidationResultDto validateAndBoardTicket(String qrCode) {
        log.info("Validating ticket QR code scan: {}", qrCode);

        if (qrCode == null || qrCode.trim().isEmpty()) {
            log.warn("Ticket validation failed: Empty QR code input");
            return ValidationResultDto.builder()
                    .valid(false)
                    .message("Invalid scan: QR code data is empty.")
                    .build();
        }

        String cleanCode = qrCode.trim();

        // Strip surrounding quotes or JSON wrappers if present
        if (cleanCode.startsWith("\"") && cleanCode.endsWith("\"") && cleanCode.length() > 2) {
            cleanCode = cleanCode.substring(1, cleanCode.length() - 1).trim();
        }

        // Check if input is wrapped in simple JSON format (e.g. {"ticketNumber":"TKT-..."})
        if (cleanCode.contains("ticketNumber") && cleanCode.contains(":")) {
            int idx = cleanCode.indexOf("ticketNumber");
            String after = cleanCode.substring(idx + 12).replaceAll("[^a-zA-Z0-9-]", "").trim();
            if (!after.isEmpty()) {
                cleanCode = after;
            }
        }

        // Lookup ticket by exact ticketNumber
        Optional<Ticket> ticketOpt = ticketRepository.findByTicketNumber(cleanCode);

        // Fallback 1: Case-insensitive lookup (e.g. tkt-2026-10001)
        if (ticketOpt.isEmpty()) {
            ticketOpt = ticketRepository.findByTicketNumberIgnoreCase(cleanCode);
        }

        // Fallback 2: Try parsing numeric ID if ticket number lookup misses
        if (ticketOpt.isEmpty()) {
            try {
                Long id = Long.parseLong(cleanCode);
                ticketOpt = ticketRepository.findById(id);
            } catch (NumberFormatException ignored) {
                // Not a numeric ID
            }
        }

        if (ticketOpt.isEmpty()) {
            log.warn("Ticket validation failed: No ticket found for QR data: {}", cleanCode);
            boardingLogRepository.save(com.trainbooking.it25100228.model.BoardingLog.builder()
                    .scannedTicketNumber(cleanCode)
                    .scanResult(com.trainbooking.it25100228.model.BoardingLog.ScanResult.NOT_FOUND)
                    .build());
            return ValidationResultDto.builder()
                    .valid(false)
                    .message("Invalid Ticket: No matching reservation found in railway database.")
                    .build();
        }

        Ticket ticket = ticketOpt.get();

        // 1. Check for duplicate boarding scan (Fraud prevention)
        if (Boolean.TRUE.equals(ticket.getIsBoarded())) {
            log.warn("Security Alert: Duplicate boarding scan detected for ticket {}", ticket.getTicketNumber());
            boardingLogRepository.save(com.trainbooking.it25100228.model.BoardingLog.builder()
                    .ticket(ticket)
                    .scannedTicketNumber(ticket.getTicketNumber())
                    .scanResult(com.trainbooking.it25100228.model.BoardingLog.ScanResult.DUPLICATE)
                    .build());
            return ValidationResultDto.builder()
                    .valid(false)
                    .message("FRAUD / DUPLICATE SCAN ALERT: Ticket " + ticket.getTicketNumber() + " has already been scanned and boarded.")
                    .ticketNumber(ticket.getTicketNumber())
                    .passengerName(ticket.getPassengerName())
                    .trainName(ticket.getTrainName())
                    .origin(ticket.getOrigin())
                    .destination(ticket.getDestination())
                    .seatClass(ticket.getSeatClass())
                    .seatNumbers(ticket.getSeatNumbers())
                    .travelDate(ticket.getTravelDate())
                    .build();
        }

        // 2. Check if booking was cancelled
        if (ticket.getBooking() != null && ticket.getBooking().getStatus() == Booking.BookingStatus.CANCELLED) {
            log.warn("Ticket validation failed: Booking for ticket {} is CANCELLED", ticket.getTicketNumber());
            boardingLogRepository.save(com.trainbooking.it25100228.model.BoardingLog.builder()
                    .ticket(ticket)
                    .scannedTicketNumber(ticket.getTicketNumber())
                    .scanResult(com.trainbooking.it25100228.model.BoardingLog.ScanResult.CANCELLED)
                    .build());
            return ValidationResultDto.builder()
                    .valid(false)
                    .message("CANCELLED TICKET ALERT: Reservation for " + ticket.getTicketNumber() + " has been cancelled.")
                    .ticketNumber(ticket.getTicketNumber())
                    .passengerName(ticket.getPassengerName())
                    .trainName(ticket.getTrainName())
                    .travelDate(ticket.getTravelDate())
                    .build();
        }

        // 3. Mark ticket as boarded
        ticket.setIsBoarded(true);
        ticketRepository.save(ticket);
        boardingLogRepository.save(com.trainbooking.it25100228.model.BoardingLog.builder()
                .ticket(ticket)
                .scannedTicketNumber(ticket.getTicketNumber())
                .scanResult(com.trainbooking.it25100228.model.BoardingLog.ScanResult.VALID)
                .build());
        log.info("Ticket {} successfully verified and marked as BOARDED.", ticket.getTicketNumber());

        return ValidationResultDto.builder()
                .valid(true)
                .message("VALID TICKET: Boarding authorized for " + ticket.getPassengerName())
                .ticketNumber(ticket.getTicketNumber())
                .passengerName(ticket.getPassengerName())
                .trainName(ticket.getTrainName())
                .origin(ticket.getOrigin())
                .destination(ticket.getDestination())
                .seatClass(ticket.getSeatClass())
                .seatNumbers(ticket.getSeatNumbers())
                .travelDate(ticket.getTravelDate())
                .build();
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
}
