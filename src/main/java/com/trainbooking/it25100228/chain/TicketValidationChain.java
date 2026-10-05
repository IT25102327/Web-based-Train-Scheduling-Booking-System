package com.trainbooking.it25100228.chain;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orchestrator configuring and executing the Ticket Validation Chain of Responsibility.
 * Links sequential validation handlers to verify barcode/QR authentications.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketValidationChain {

    private final EmptyQrValidationHandler emptyQrHandler;
    private final TicketLookupValidationHandler lookupHandler;
    private final DuplicateBoardingValidationHandler duplicateHandler;
    private final CancelledBookingValidationHandler cancelledHandler;
    private final BoardingApprovalValidationHandler approvalHandler;

    private TicketValidationHandler chainHead;

    @PostConstruct
    public void initChain() {
        emptyQrHandler.setNext(lookupHandler)
                .setNext(duplicateHandler)
                .setNext(cancelledHandler)
                .setNext(approvalHandler);

        chainHead = emptyQrHandler;
        log.info("Initialized TicketValidationChain pipeline with {} handlers.", 5);
    }

    /**
     * Executes the ticket validation pipeline for the given raw QR code string.
     *
     * @param rawQrCode scanned QR data or ticket number
     * @return {@link ValidationResultDto} outcome
     */
    public ValidationResultDto execute(String rawQrCode) {
        TicketValidationContext context = TicketValidationContext.builder()
                .rawQrCode(rawQrCode)
                .build();

        if (chainHead == null) {
            initChain();
        }

        chainHead.handle(context);
        return context.getResult();
    }
}
