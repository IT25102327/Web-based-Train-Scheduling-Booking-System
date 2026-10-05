package com.trainbooking.it25100977.strategy;

import com.trainbooking.it25100977.dto.PaymentRequest;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25103308.model.Booking;
import org.springframework.web.multipart.MultipartFile;

/**
 * Strategy interface defining the contract for processing train booking payments.
 * Implementations handle specific payment execution channels (e.g. Card, Bank Slip).
 *
 * @author SLIIT Software Engineering Team (IT25100977)
 * @version 1.0.0
 */
public interface PaymentStrategy {

    /**
     * Executes payment processing according to the specific payment strategy.
     *
     * @param booking the booking entity being paid for
     * @param request the payment request payload containing credentials or transaction metadata
     * @param slipFile optional uploaded proof-of-payment document or image
     * @return the persisted Payment record
     */
    Payment processPayment(Booking booking, PaymentRequest request, MultipartFile slipFile);

    /**
     * Identifies the payment method handled by this strategy.
     *
     * @return {@link Payment.PaymentMethod}
     */
    Payment.PaymentMethod getSupportedMethod();
}
