package com.trainbooking.it25100977.strategy;

import com.trainbooking.it25100977.dto.PaymentRequest;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.repository.PaymentRepository;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Concrete strategy implementing credit and debit card payment processing.
 * Validates card credentials via the Luhn algorithm checksum, simulates payment gateway responses,
 * and records card transaction references.
 *
 * @author SLIIT Software Engineering Team (IT25100977)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentStrategy {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Override
    public Payment.PaymentMethod getSupportedMethod() {
        return Payment.PaymentMethod.CARD;
    }

    @Override
    public Payment processPayment(Booking booking, PaymentRequest request, MultipartFile slipFile) {
        log.info("Executing CardPaymentStrategy for booking ID: {}", booking.getId());

        String cleanCard = (request != null && request.getCardNumber() != null)
                ? request.getCardNumber().replaceAll("[\\s\\-]+", "") : "";

        // Simulated card decline check (card ending in 0000 or CVV 000)
        if (cleanCard.endsWith("0000") || (request != null && "000".equals(request.getCvv()))) {
            log.warn("Payment declined for booking ID: {} - simulated decline criteria matched.", booking.getId());
            Payment failedPayment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .status(Payment.PaymentStatus.FAILED)
                    .transactionRef("FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .paymentMethod(Payment.PaymentMethod.CARD)
                    .build();
            paymentRepository.save(failedPayment);
            throw new IllegalArgumentException("Card transaction declined by payment gateway: Insufficient funds or invalid card credentials. Please retry with a valid card before your 10-minute seat lock expires.");
        }

        // Validate card credentials if card details were provided
        if (request != null && (request.getCardNumber() != null && !request.getCardNumber().isBlank()
                || "CARD".equalsIgnoreCase(request.getPaymentMethod()) && request.getCardNumber() != null)) {
            validateCreditCard(request);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(Payment.PaymentMethod.CARD)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Card payment successfully verified and recorded: {}", savedPayment.getTransactionRef());
        return savedPayment;
    }

    /**
     * Validates cardholder name, card number with Luhn check, expiration date, and CVV.
     *
     * @param request payment request
     */
    public void validateCreditCard(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment details cannot be null.");
        }

        if (request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Cardholder name is required.");
        }

        String rawCard = request.getCardNumber();
        if (rawCard == null || rawCard.trim().isEmpty()) {
            throw new IllegalArgumentException("Credit card number is required.");
        }

        String cleanCard = rawCard.replaceAll("[\\s\\-]+", "");

        if (!cleanCard.matches("^\\d+$")) {
            throw new IllegalArgumentException("Invalid credit card number: Must contain only numeric digits.");
        }

        if (cleanCard.length() < 13 || cleanCard.length() > 19) {
            throw new IllegalArgumentException("Invalid credit card number: Length must be between 13 and 19 digits.");
        }

        if (!isValidLuhn(cleanCard)) {
            throw new IllegalArgumentException("Invalid credit card number: Checksum validation failed (Luhn Algorithm). Please check the digits and try again.");
        }

        if (request.getExpiryMonth() != null && request.getExpiryYear() != null
                && !request.getExpiryMonth().isBlank() && !request.getExpiryYear().isBlank()) {
            try {
                int month = Integer.parseInt(request.getExpiryMonth().trim());
                int year = Integer.parseInt(request.getExpiryYear().trim());
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("Invalid expiration month: Must be between 01 and 12.");
                }
                YearMonth currentYearMonth = YearMonth.now();
                YearMonth cardExpiry = YearMonth.of(year, month);
                if (cardExpiry.isBefore(currentYearMonth)) {
                    throw new IllegalArgumentException("Credit card has expired (" + request.getExpiryMonth() + "/" + request.getExpiryYear() + ").");
                }
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid expiration date format.");
            }
        }

        if (request.getCvv() != null && !request.getCvv().trim().isEmpty()) {
            String cleanCvv = request.getCvv().trim();
            if (!cleanCvv.matches("^\\d{3,4}$")) {
                throw new IllegalArgumentException("Invalid CVV/CVC code: Must be 3 or 4 numeric digits.");
            }
        }
    }

    /**
     * Evaluates Luhn algorithm (Mod 10 Checksum) on a numeric card string.
     *
     * @param cardNumber numeric digits
     * @return true if valid
     */
    public static boolean isValidLuhn(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (digit < 0 || digit > 9) {
                return false;
            }
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            sum += digit;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
