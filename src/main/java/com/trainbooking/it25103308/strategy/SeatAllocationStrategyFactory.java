package com.trainbooking.it25103308.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory class resolving the appropriate seat allocation strategy based on the booking request input.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatAllocationStrategyFactory {

    private final List<SeatAllocationStrategy> strategies;

    /**
     * Resolves the matching strategy for the given requested seat string.
     *
     * @param requestedSeats passenger-specified seats string
     * @return matching {@link SeatAllocationStrategy}
     */
    public SeatAllocationStrategy getStrategy(String requestedSeats) {
        for (SeatAllocationStrategy s : strategies) {
            if (s.supports(requestedSeats)) {
                return s;
            }
        }
        return strategies.get(0);
    }
}
