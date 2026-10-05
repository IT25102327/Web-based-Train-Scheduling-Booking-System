package com.trainbooking.it25100977.strategy;

import com.trainbooking.it25100977.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class for resolving the appropriate {@link PaymentStrategy} based on the requested payment method.
 * Implements the GoF Factory Pattern in conjunction with the Strategy Pattern.
 *
 * @author SLIIT Software Engineering Team (IT25100977)
 * @version 1.0.0
 */
@Slf4j
@Component
public class PaymentStrategyFactory {

    private final Map<Payment.PaymentMethod, PaymentStrategy> strategies = new EnumMap<>(Payment.PaymentMethod.class);

    public PaymentStrategyFactory(List<PaymentStrategy> strategyList) {
        for (PaymentStrategy strategy : strategyList) {
            strategies.put(strategy.getSupportedMethod(), strategy);
            log.info("Registered payment strategy: {} -> {}", strategy.getSupportedMethod(), strategy.getClass().getSimpleName());
        }
    }

    /**
     * Resolves the strategy for the given payment method string (e.g. "CARD", "MANUAL_SLIP").
     * Defaults to CardPaymentStrategy if method is unspecified or unrecognized.
     *
     * @param paymentMethodStr method name string
     * @return matching {@link PaymentStrategy}
     */
    public PaymentStrategy getStrategy(String paymentMethodStr) {
        if (paymentMethodStr == null || paymentMethodStr.isBlank()) {
            return getStrategy(Payment.PaymentMethod.CARD);
        }

        try {
            Payment.PaymentMethod method = Payment.PaymentMethod.valueOf(paymentMethodStr.trim().toUpperCase());
            return getStrategy(method);
        } catch (IllegalArgumentException ex) {
            log.warn("Unrecognized payment method '{}', defaulting to CARD strategy", paymentMethodStr);
            return getStrategy(Payment.PaymentMethod.CARD);
        }
    }

    /**
     * Resolves the strategy for the strongly typed {@link Payment.PaymentMethod} enum.
     *
     * @param method payment method enum
     * @return matching {@link PaymentStrategy}
     */
    public PaymentStrategy getStrategy(Payment.PaymentMethod method) {
        PaymentStrategy strategy = strategies.get(method);
        if (strategy == null) {
            // Fallback to CARD if available
            strategy = strategies.get(Payment.PaymentMethod.CARD);
            if (strategy == null) {
                throw new IllegalStateException("No payment strategy registered for method: " + method);
            }
        }
        return strategy;
    }
}
