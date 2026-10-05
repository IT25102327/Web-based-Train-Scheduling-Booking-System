package com.trainbooking.it25100977.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.web.multipart.MultipartFile;

/**
 * Data Transfer Object containing payment checkout details for card and manual slip methods.
 *
 * @author SLIIT Software Engineering Team (IT25100977)
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private Long bookingId;

    @Builder.Default
    private String paymentMethod = "CARD";

    // Card Details
    private String cardNumber;
    private String cardHolderName;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    // Manual Bank Transfer / Deposit Slip Details
    private String bankName;
    private String bankReference;
    private String depositorName;
    private String depositDate;
    private String slipFileName;
    private MultipartFile slipFile;
}
