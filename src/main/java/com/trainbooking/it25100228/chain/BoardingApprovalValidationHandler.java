package com.trainbooking.it25100228.chain;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100228.model.BoardingLog;
import com.trainbooking.it25100228.repository.BoardingLogRepository;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Terminal handler in the validation chain that marks the ticket as boarded,
 * records the audit log, and constructs the authorized validation response.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardingApprovalValidationHandler extends TicketValidationHandler {

    private final TicketRepository ticketRepository;
    private final BoardingLogRepository boardingLogRepository;

    @Override
    public void handle(TicketValidationContext context) {
        Ticket ticket = context.getTicket();

        // Mark ticket as boarded
        ticket.setIsBoarded(true);
        ticketRepository.save(ticket);

        boardingLogRepository.save(BoardingLog.builder()
                .ticket(ticket)
                .scannedTicketNumber(ticket.getTicketNumber())
                .scanResult(BoardingLog.ScanResult.VALID)
                .build());

        log.info("Ticket {} successfully verified and marked as BOARDED.", ticket.getTicketNumber());

        context.terminate(ValidationResultDto.builder()
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
                .build());
    }
}
