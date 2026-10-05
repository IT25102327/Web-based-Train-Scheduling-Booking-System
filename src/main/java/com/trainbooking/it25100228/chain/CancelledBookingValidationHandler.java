package com.trainbooking.it25100228.chain;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100228.model.BoardingLog;
import com.trainbooking.it25100228.repository.BoardingLogRepository;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25103308.model.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Checks whether the ticket belongs to a reservation that has been cancelled or refunded.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelledBookingValidationHandler extends TicketValidationHandler {

    private final BoardingLogRepository boardingLogRepository;

    @Override
    public void handle(TicketValidationContext context) {
        Ticket ticket = context.getTicket();

        if (ticket.getBooking() != null && ticket.getBooking().getStatus() == Booking.BookingStatus.CANCELLED) {
            log.warn("Ticket validation failed: Booking for ticket {} is CANCELLED", ticket.getTicketNumber());
            boardingLogRepository.save(BoardingLog.builder()
                    .ticket(ticket)
                    .scannedTicketNumber(ticket.getTicketNumber())
                    .scanResult(BoardingLog.ScanResult.CANCELLED)
                    .build());

            context.terminate(ValidationResultDto.builder()
                    .valid(false)
                    .message("CANCELLED TICKET ALERT: Reservation for " + ticket.getTicketNumber() + " has been cancelled.")
                    .ticketNumber(ticket.getTicketNumber())
                    .passengerName(ticket.getPassengerName())
                    .trainName(ticket.getTrainName())
                    .travelDate(ticket.getTravelDate())
                    .build());
            return;
        }

        passToNext(context);
    }
}
