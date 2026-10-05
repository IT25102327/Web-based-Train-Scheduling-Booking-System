package com.trainbooking.it25100228.chain;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100228.model.BoardingLog;
import com.trainbooking.it25100228.repository.BoardingLogRepository;
import com.trainbooking.it25100977.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Checks for duplicate boarding attempts to prevent ticket fraud at turnstiles.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateBoardingValidationHandler extends TicketValidationHandler {

    private final BoardingLogRepository boardingLogRepository;

    @Override
    public void handle(TicketValidationContext context) {
        Ticket ticket = context.getTicket();

        if (Boolean.TRUE.equals(ticket.getIsBoarded())) {
            log.warn("Security Alert: Duplicate boarding scan detected for ticket {}", ticket.getTicketNumber());
            boardingLogRepository.save(BoardingLog.builder()
                    .ticket(ticket)
                    .scannedTicketNumber(ticket.getTicketNumber())
                    .scanResult(BoardingLog.ScanResult.DUPLICATE)
                    .build());

            context.terminate(ValidationResultDto.builder()
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
                    .build());
            return;
        }

        passToNext(context);
    }
}
