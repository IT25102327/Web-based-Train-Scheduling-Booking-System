package com.trainbooking.it25103308;

import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.state.*;
import com.trainbooking.it25103308.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying State Pattern for booking lifecycle transitions
 * and Strategy Pattern for intelligent seat allocations.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
class BookingStateAndStrategyTest {

    private PendingBookingState pendingState;
    private ConfirmedBookingState confirmedState;
    private CancelledBookingState cancelledState;
    private BookingStateFactory stateFactory;

    private SelectedSeatAllocationStrategy selectedSeatStrategy;
    private AutoSequentialSeatAllocationStrategy autoSeatStrategy;
    private SeatAllocationStrategyFactory seatStrategyFactory;

    @BeforeEach
    void setUp() {
        pendingState = new PendingBookingState();
        confirmedState = new ConfirmedBookingState();
        cancelledState = new CancelledBookingState();
        stateFactory = new BookingStateFactory(pendingState, confirmedState, cancelledState);

        selectedSeatStrategy = new SelectedSeatAllocationStrategy();
        autoSeatStrategy = new AutoSequentialSeatAllocationStrategy();
        seatStrategyFactory = new SeatAllocationStrategyFactory(List.of(selectedSeatStrategy, autoSeatStrategy));
    }

    @Test
    @DisplayName("State Factory should map BookingStatus enum to matching BookingState")
    void testStateFactoryMapping() {
        Booking bPending = Booking.builder().status(Booking.BookingStatus.PENDING).build();
        assertEquals(Booking.BookingStatus.PENDING, stateFactory.getState(bPending).getStatus());
        assertTrue(stateFactory.getState(bPending).canCancel());

        Booking bConfirmed = Booking.builder().status(Booking.BookingStatus.CONFIRMED).build();
        assertEquals(Booking.BookingStatus.CONFIRMED, stateFactory.getState(bConfirmed).getStatus());
        assertTrue(stateFactory.getState(bConfirmed).canCancel());

        Booking bCancelled = Booking.builder().status(Booking.BookingStatus.CANCELLED).build();
        assertEquals(Booking.BookingStatus.CANCELLED, stateFactory.getState(bCancelled).getStatus());
        assertFalse(stateFactory.getState(bCancelled).canCancel());
    }

    @Test
    @DisplayName("CancelledBookingState should reject invalid state transitions")
    void testCancelledStateTransitions() {
        Booking booking = Booking.builder().id(12L).status(Booking.BookingStatus.CANCELLED).build();

        assertThrows(IllegalStateException.class, () -> cancelledState.confirm(booking));
        assertThrows(IllegalStateException.class, () -> cancelledState.cancel(booking));
    }

    @Test
    @DisplayName("PendingBookingState should successfully transition to CONFIRMED and CANCELLED")
    void testPendingStateTransitions() {
        Booking b1 = Booking.builder().id(1L).status(Booking.BookingStatus.PENDING).build();
        pendingState.confirm(b1);
        assertEquals(Booking.BookingStatus.CONFIRMED, b1.getStatus());

        Booking b2 = Booking.builder().id(2L).status(Booking.BookingStatus.PENDING).build();
        pendingState.cancel(b2);
        assertEquals(Booking.BookingStatus.CANCELLED, b2.getStatus());
    }

    @Test
    @DisplayName("Seat Allocation Strategy Factory should resolve selected vs auto strategies")
    void testSeatStrategyFactory() {
        SeatAllocationStrategy explicit = seatStrategyFactory.getStrategy("Coach 01 / S05");
        assertInstanceOf(SelectedSeatAllocationStrategy.class, explicit);

        SeatAllocationStrategy auto = seatStrategyFactory.getStrategy(null);
        assertInstanceOf(AutoSequentialSeatAllocationStrategy.class, auto);

        SeatAllocationStrategy emptyStr = seatStrategyFactory.getStrategy("   ");
        assertInstanceOf(AutoSequentialSeatAllocationStrategy.class, emptyStr);
    }

    @Test
    @DisplayName("SelectedSeatAllocationStrategy should reject already reserved seats")
    void testSelectedSeatStrategy_ConflictDetection() {
        List<String> booked = List.of("Coach 01 / S01", "Coach 01 / S02");

        assertThrows(IllegalStateException.class, () ->
                selectedSeatStrategy.allocateSeats(Booking.SeatClass.FIRST, 1, "Coach 01 / S01", 10, booked));

        // Available seat should pass
        String allocated = selectedSeatStrategy.allocateSeats(
                Booking.SeatClass.FIRST, 1, "Coach 01 / S09", 10, booked);
        assertEquals("Coach 01 / S09", allocated);
    }

    @Test
    @DisplayName("AutoSequentialSeatAllocationStrategy should assign consecutive coach seats")
    void testAutoSequentialSeatStrategy() {
        String result = autoSeatStrategy.allocateSeats(
                Booking.SeatClass.SECOND, 3, null, 25, new ArrayList<>());

        assertNotNull(result);
        assertTrue(result.contains("Car 02 / S26 - S28"));
    }
}
