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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User sampleUser;
    private Train sampleTrain;
    private Route sampleRoute;
    private Schedule sampleSchedule;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .firstName("Kasun")
                .lastName("Perera")
                .email("passenger@trainbooking.lk")
                .build();

        sampleTrain = Train.builder()
                .id(1L)
                .trainNumber("1015")
                .trainName("Udarata Menike")
                .firstClassSeats(60)
                .secondClassSeats(240)
                .build();

        sampleRoute = Route.builder()
                .id(1L)
                .origin("Colombo Fort")
                .destination("Kandy")
                .build();

        sampleSchedule = Schedule.builder()
                .id(10L)
                .train(sampleTrain)
                .route(sampleRoute)
                .firstClassFare(new BigDecimal("1500.00"))
                .secondClassFare(new BigDecimal("800.00"))
                .build();
    }

    @Test
    @DisplayName("Should calculate remaining available seats correctly")
    void testGetAvailableSeats() {
        LocalDate travelDate = LocalDate.now().plusDays(1);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(sampleSchedule));
        when(bookingRepository.countByScheduleIdAndTravelDateAndSeatClass(10L, travelDate, Booking.SeatClass.FIRST))
                .thenReturn(15);

        int available = bookingService.getAvailableSeats(10L, travelDate, "FIRST");

        assertEquals(45, available); // 60 total - 15 booked = 45 remaining
    }

    @Test
    @DisplayName("Should successfully create a booking with temporary lock")
    void testCreateBooking_Success() {
        LocalDate travelDate = LocalDate.now().plusDays(2);
        BookingRequest request = BookingRequest.builder()
                .scheduleId(10L)
                .travelDate(travelDate)
                .seatClass("FIRST")
                .numberOfSeats(2)
                .passengerName("Kasun Perera")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(sampleSchedule));
        when(bookingRepository.countByScheduleIdAndTravelDateAndSeatClass(10L, travelDate, Booking.SeatClass.FIRST))
                .thenReturn(10);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(100L);
            return b;
        });

        Booking result = bookingService.createBooking(request, 1L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(new BigDecimal("3000.00"), result.getTotalAmount()); // 1500.00 * 2
        assertEquals(Booking.BookingStatus.PENDING, result.getStatus());
        assertNotNull(result.getLockExpiresAt());
        assertNotNull(result.getSeatNumbers());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when requested seats exceed capacity")
    void testCreateBooking_InsufficientSeats() {
        LocalDate travelDate = LocalDate.now().plusDays(2);
        BookingRequest request = BookingRequest.builder()
                .scheduleId(10L)
                .travelDate(travelDate)
                .seatClass("FIRST")
                .numberOfSeats(10)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(sampleSchedule));
        when(bookingRepository.countByScheduleIdAndTravelDateAndSeatClass(10L, travelDate, Booking.SeatClass.FIRST))
                .thenReturn(55); // Only 5 available (60 - 55 = 5)

        assertThrows(IllegalStateException.class, () -> bookingService.createBooking(request, 1L));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should return booked seat numbers for seat map display")
    void testGetBookedSeatNumbers() {
        LocalDate travelDate = LocalDate.now();
        Booking b1 = Booking.builder()
                .schedule(sampleSchedule)
                .travelDate(travelDate)
                .seatClass(Booking.SeatClass.FIRST)
                .status(Booking.BookingStatus.CONFIRMED)
                .seatNumbers("Car 01 / S01, Car 01 / S02")
                .build();
        when(bookingRepository.findAll()).thenReturn(java.util.List.of(b1));

        java.util.List<String> seats = bookingService.getBookedSeatNumbers(10L, travelDate, "FIRST");
        assertNotNull(seats);
        assertEquals(2, seats.size());
        assertTrue(seats.contains("Car 01 / S01"));
        assertTrue(seats.contains("Car 01 / S02"));
    }
}
