package com.trainbooking.it25100228.chain;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100977.model.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context object carrying state across handlers in the Ticket Validation Chain of Responsibility.
 *
 * @author SLIIT Software Engineering Team (IT25100228)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketValidationContext {

    private String rawQrCode;
    private String cleanCode;
    private Ticket ticket;
    private ValidationResultDto result;
    private boolean handled;

    /**
     * Marks the validation process as concluded with the given result.
     *
     * @param validationResult final validation result DTO
     */
    public void terminate(ValidationResultDto validationResult) {
        this.result = validationResult;
        this.handled = true;
    }
}
