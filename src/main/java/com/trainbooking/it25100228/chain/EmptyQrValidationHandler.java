package com.trainbooking.it25100228.chain;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates that the scanned QR code or input ticket string is non-empty.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
@Slf4j
@Component
public class EmptyQrValidationHandler extends TicketValidationHandler {

    @Override
    public void handle(TicketValidationContext context) {
        String raw = context.getRawQrCode();
        if (raw == null || raw.trim().isEmpty()) {
            log.warn("Ticket validation failed: Empty QR code input");
            context.terminate(ValidationResultDto.builder()
                    .valid(false)
                    .message("Invalid scan: QR code data is empty.")
                    .build());
            return;
        }

        passToNext(context);
    }
}
