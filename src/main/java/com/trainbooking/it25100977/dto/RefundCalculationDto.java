package com.trainbooking.it25100977.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object encapsulating pre-cancellation refund calculation metrics and policy tier details.
 *
 * @author SLIIT Software Engineering Team (IT25100977 - Shehara D.M.D.)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCalculationDto {

    private BigDecimal originalAmount;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private int refundPercentage;
    private long hoursUntilDeparture;
    private String policyTierDescription;
    private boolean eligibleForRefund;
}
