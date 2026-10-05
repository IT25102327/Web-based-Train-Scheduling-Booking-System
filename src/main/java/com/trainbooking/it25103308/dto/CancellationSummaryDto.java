package com.trainbooking.it25103308.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object presenting comprehensive booking cancellation and refund estimate breakdown.
 *
 * @author SLIIT Software Engineering Team (IT25103308 - Anfas M.S.)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationSummaryDto {

    private Long bookingId;
    private String ticketNumber;
    private String passengerName;
    private String trainName;
    private String origin;
    private String destination;
    private LocalDate travelDate;
    private String departureTime;
    private String seatNumbers;
    private String seatClass;
    private Integer numberOfSeats;

    private BigDecimal originalAmount;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private int refundPercentage;
    private String policyTierDescription;
    private boolean eligibleForRefund;
    private boolean canCancel;
}
