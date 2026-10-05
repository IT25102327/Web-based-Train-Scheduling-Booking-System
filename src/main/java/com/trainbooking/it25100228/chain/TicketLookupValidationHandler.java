package com.trainbooking.it25100228.chain;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100228.model.BoardingLog;
import com.trainbooking.it25100228.repository.BoardingLogRepository;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Sanitizes the raw QR input string, extracts the clean ticket number or numeric ID,
 * and performs database resolution against {@link TicketRepository}.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketLookupValidationHandler extends TicketValidationHandler {

    private final TicketRepository ticketRepository;
    private final BoardingLogRepository boardingLogRepository;

    @Override
    public void handle(TicketValidationContext context) {
        String cleanCode = context.getRawQrCode().trim();

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

        context.setCleanCode(cleanCode);

        // Lookup ticket by exact ticketNumber
        Optional<Ticket> ticketOpt = ticketRepository.findByTicketNumber(cleanCode);

        // Fallback 1: Case-insensitive lookup
        if (ticketOpt.isEmpty()) {
            ticketOpt = ticketRepository.findByTicketNumberIgnoreCase(cleanCode);
        }

        // Fallback 2: Try parsing numeric ID
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
            boardingLogRepository.save(BoardingLog.builder()
                    .scannedTicketNumber(cleanCode)
                    .scanResult(BoardingLog.ScanResult.NOT_FOUND)
                    .build());
            context.terminate(ValidationResultDto.builder()
                    .valid(false)
                    .message("Invalid Ticket: No matching reservation found in railway database.")
                    .build());
            return;
        }

        context.setTicket(ticketOpt.get());
        passToNext(context);
    }
}
