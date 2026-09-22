package com.trainbooking.it25103308;

import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.repository.UserRepository;
import com.trainbooking.it25102327.model.Route;
import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.ScheduleRepository;
import com.trainbooking.it25103308.dto.BookingRequest;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import com.trainbooking.it25103308.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Concurrency and seat lock verification tests simulating simultaneous booking requests.
 *
 * @author SLIIT Software Engineering Team (IT25103308)
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class BookingConcurrencyTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private Schedule schedule;
    private User passenger;

    @BeforeEach
    void setUp() {
        Train train = Train.builder()
                .id(1L)
                .trainNumber("1015")
                .trainName("Udarata Menike")
                .firstClassSeats(2) // Only 2 seats total available for testing concurrency bounds
                .secondClassSeats(4)
                .build();

        schedule = Schedule.builder()
                .id(50L)
                .train(train)
                .firstClassFare(new BigDecimal("1800.00"))
                .secondClassFare(new BigDecimal("1000.00"))
                .build();

        passenger = User.builder()
                .id(1L)
                .firstName("Kasun")
                .lastName("Perera")
                .email("passenger@trainbooking.lk")
                .phone("+94771112233")
                .build();
    }

    @Test
    @DisplayName("Should simulate concurrent booking attempts and reject when capacity exceeded")
    void testConcurrentBookingAttempts_EnforcesCapacityBoundaries() throws InterruptedException {
        when(scheduleRepository.findById(50L)).thenReturn(Optional.of(schedule));
        when(userRepository.findById(1L)).thenReturn(Optional.of(passenger));

        // Atomic tracker of booked seats
        AtomicInteger currentlyBooked = new AtomicInteger(0);

        when(bookingRepository.countByScheduleIdAndTravelDateAndSeatClass(eq(50L), any(LocalDate.class), eq(Booking.SeatClass.FIRST)))
                .thenAnswer(inv -> currentlyBooked.get());

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId((long) (currentlyBooked.incrementAndGet()));
            return b;
        });

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Boolean> outcomes = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // wait for all threads to start simultaneously
                    BookingRequest req = new BookingRequest(50L, LocalDate.now(), "FIRST", 1);
                    bookingService.createBooking(req, 1L);
                    outcomes.add(true);
                } catch (Exception ex) {
                    outcomes.add(false);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // trigger all threads at once
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        long successfulBookings = outcomes.stream().filter(b -> b).count();
        long rejectedBookings = outcomes.stream().filter(b -> !b).count();

        // Exactly 2 bookings should succeed, and 8 should be rejected due to capacity limit
        assertEquals(2, successfulBookings);
        assertEquals(8, rejectedBookings);
    }

    @Test
    @DisplayName("Should release expired seat locks automatically")
    void testReleaseExpiredLocks() {
        Booking expiredBooking = Booking.builder()
                .id(101L)
                .status(Booking.BookingStatus.PENDING)
                .lockExpiresAt(LocalDateTime.now().minusMinutes(5)) // expired 5 mins ago
                .build();

        Booking activeBooking = Booking.builder()
                .id(102L)
                .status(Booking.BookingStatus.PENDING)
                .lockExpiresAt(LocalDateTime.now().plusMinutes(5)) // active
                .build();

        when(bookingRepository.findAll()).thenReturn(List.of(expiredBooking, activeBooking));

        bookingService.releaseExpiredLocks();

        assertEquals(Booking.BookingStatus.CANCELLED, expiredBooking.getStatus());
        assertEquals(Booking.BookingStatus.PENDING, activeBooking.getStatus());
        verify(bookingRepository, times(1)).save(expiredBooking);
        verify(bookingRepository, never()).save(activeBooking);
    }

    @Test
    @DisplayName("Should explicitly release seat lock when user cancels checkout")
    void testExplicitReleaseLock() {
        Booking pendingBooking = Booking.builder()
                .id(105L)
                .status(Booking.BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(105L)).thenReturn(Optional.of(pendingBooking));

        bookingService.releaseLock(105L);

        assertEquals(Booking.BookingStatus.CANCELLED, pendingBooking.getStatus());
        verify(bookingRepository, times(1)).save(pendingBooking);
    }
}
