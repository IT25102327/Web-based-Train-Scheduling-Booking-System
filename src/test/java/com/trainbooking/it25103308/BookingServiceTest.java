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

    @Mock
    private com.trainbooking.it25100977.service.PaymentService paymentService;

    @Mock
    private com.trainbooking.it25102925.service.NotificationService notificationService;

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

    @Test
    @DisplayName("Should cancel confirmed booking, restore seat capacity, and process refund")
    void testCancelBooking_Confirmed_Success() {
        Booking booking = Booking.builder()
                .id(100L)
                .passenger(sampleUser)
                .schedule(sampleSchedule)
                .status(Booking.BookingStatus.CONFIRMED)
                .travelDate(LocalDate.now().plusDays(3))
                .seatClass(Booking.SeatClass.FIRST)
                .numberOfSeats(2)
                .totalAmount(new BigDecimal("3000.00"))
                .seatNumbers("Car 01 / S01 - S02")
                .build();

        com.trainbooking.it25100977.model.Refund mockRefund = com.trainbooking.it25100977.model.Refund.builder()
                .id(50L)
                .booking(booking)
                .originalAmount(new BigDecimal("3000.00"))
                .refundAmount(new BigDecimal("2700.00"))
                .cancellationFee(new BigDecimal("300.00"))
                .refundPercentage(90)
                .refundTransactionRef("REF-TEST-1234")
                .status(com.trainbooking.it25100977.model.Refund.RefundStatus.COMPLETED)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(paymentService.processRefund(eq(100L), any())).thenReturn(mockRefund);

        com.trainbooking.it25100977.model.Refund result = bookingService.cancelBooking(100L, 1L, "Schedule change");

        assertNotNull(result);
        assertEquals(Booking.BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("REF-TEST-1234", result.getRefundTransactionRef());
        assertEquals(new BigDecimal("2700.00"), result.getRefundAmount());

        verify(bookingRepository, times(1)).save(booking);
        verify(paymentService, times(1)).processRefund(eq(100L), any());
        verify(notificationService, times(1)).sendCancellationNotification(eq(booking), eq(mockRefund));
    }

    @Test
    @DisplayName("Should reject cancellation if booking is already cancelled")
    void testCancelBooking_AlreadyCancelled_ThrowsException() {
        Booking booking = Booking.builder()
                .id(100L)
                .passenger(sampleUser)
                .status(Booking.BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> bookingService.cancelBooking(100L, 1L, "Changed mind"));
        verify(paymentService, never()).processRefund(any(), any());
    }

    @Test
    @DisplayName("Should reject cancellation if requested by another passenger")
    void testCancelBooking_Unauthorized_ThrowsSecurityException() {
        Booking booking = Booking.builder()
                .id(100L)
                .passenger(sampleUser)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(SecurityException.class, () -> bookingService.cancelBooking(100L, 999L, "Hack attempt"));
        verify(bookingRepository, never()).save(any());
        verify(paymentService, never()).processRefund(any(), any());
    }

    @Test
    @DisplayName("Should calculate pre-cancellation refund quote accurately")
    void testGetCancellationSummary_Success() {
        Booking booking = Booking.builder()
                .id(100L)
                .passenger(sampleUser)
                .passengerName("Kasun Perera")
                .schedule(sampleSchedule)
                .status(Booking.BookingStatus.CONFIRMED)
                .travelDate(LocalDate.now().plusDays(2))
                .seatClass(Booking.SeatClass.FIRST)
                .numberOfSeats(1)
                .totalAmount(new BigDecimal("1500.00"))
                .seatNumbers("Car 01 / S01")
                .build();

        com.trainbooking.it25100977.dto.RefundCalculationDto calc = com.trainbooking.it25100977.dto.RefundCalculationDto.builder()
                .originalAmount(new BigDecimal("1500.00"))
                .refundAmount(new BigDecimal("1350.00"))
                .cancellationFee(new BigDecimal("150.00"))
                .refundPercentage(90)
                .hoursUntilDeparture(48)
                .policyTierDescription("Tier 1 (>48h): 90% Refund")
                .eligibleForRefund(true)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(paymentService.calculateRefund(booking)).thenReturn(calc);

        com.trainbooking.it25103308.dto.CancellationSummaryDto summary = bookingService.getCancellationSummary(100L, 1L);

        assertNotNull(summary);
        assertEquals(100L, summary.getBookingId());
        assertEquals("Kasun Perera", summary.getPassengerName());
        assertEquals(new BigDecimal("1350.00"), summary.getRefundAmount());
        assertEquals(90, summary.getRefundPercentage());
        assertTrue(summary.isCanCancel());
        assertTrue(summary.isEligibleForRefund());
    }

    @Test
    @DisplayName("Should generate train coaches correctly categorized into 1st Class and 2nd Class")
    void testGenerateTrainCoaches_CategorizedIntoFirstAndSecondClass() {
        LocalDate date = LocalDate.now();
        when(bookingRepository.findAll()).thenReturn(java.util.Collections.emptyList());

        java.util.List<com.trainbooking.it25103308.dto.CoachDto> coaches = bookingService.generateTrainCoaches(sampleSchedule, date);

        assertNotNull(coaches);
        assertFalse(coaches.isEmpty());

        long firstClassCount = coaches.stream().filter(c -> "FIRST".equals(c.getCoachClass())).count();
        long secondClassCount = coaches.stream().filter(c -> "SECOND".equals(c.getCoachClass())).count();

        assertTrue(firstClassCount >= 1, "Must contain at least 1 First Class coach");
        assertTrue(secondClassCount >= 1, "Must contain at least 1 Second Class coach");

        com.trainbooking.it25103308.dto.CoachDto firstCoach = coaches.stream()
                .filter(c -> "FIRST".equals(c.getCoachClass()))
                .findFirst()
                .orElseThrow();
        assertEquals("2+1", firstCoach.getLayoutType());
        assertEquals(20, firstCoach.getTotalSeats());
        assertEquals(20, firstCoach.getSeats().size());

        com.trainbooking.it25103308.dto.CoachDto secondCoach = coaches.stream()
                .filter(c -> "SECOND".equals(c.getCoachClass()))
                .findFirst()
                .orElseThrow();
        assertEquals("2+2", secondCoach.getLayoutType());
        assertEquals(32, secondCoach.getTotalSeats());
        assertEquals(32, secondCoach.getSeats().size());
    }

    @Test
    @DisplayName("Should successfully create booking with user-selected custom seat positions")
    void testCreateBooking_ExplicitSeatSelection_Success() {
        LocalDate travelDate = LocalDate.now().plusDays(2);
        BookingRequest request = BookingRequest.builder()
                .scheduleId(10L)
                .travelDate(travelDate)
                .seatClass("FIRST")
                .numberOfSeats(2)
                .seatNumbers("Coach 01 / S03, Coach 01 / S04")
                .coachNumber("Coach 01")
                .passengerName("Kasun Perera")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(sampleSchedule));
        when(bookingRepository.countByScheduleIdAndTravelDateAndSeatClass(10L, travelDate, Booking.SeatClass.FIRST))
                .thenReturn(5);
        when(bookingRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(200L);
            return b;
        });

        Booking result = bookingService.createBooking(request, 1L);

        assertNotNull(result);
        assertEquals("Coach 01 / S03, Coach 01 / S04", result.getSeatNumbers());
        assertEquals(Booking.BookingStatus.PENDING, result.getStatus());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking if any requested seat is already reserved by another passenger")
    void testCreateBooking_SeatAlreadyReserved_ThrowsConflictException() {
        LocalDate travelDate = LocalDate.now().plusDays(2);
        Booking existing = Booking.builder()
                .schedule(sampleSchedule)
                .travelDate(travelDate)
                .seatClass(Booking.SeatClass.FIRST)
                .status(Booking.BookingStatus.CONFIRMED)
                .seatNumbers("Coach 01 / S03")
                .build();

        BookingRequest request = BookingRequest.builder()
                .scheduleId(10L)
                .travelDate(travelDate)
                .seatClass("FIRST")
                .numberOfSeats(2)
                .seatNumbers("Coach 01 / S03, Coach 01 / S04")
                .coachNumber("Coach 01")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(sampleSchedule));
        when(bookingRepository.countByScheduleIdAndTravelDateAndSeatClass(10L, travelDate, Booking.SeatClass.FIRST))
                .thenReturn(1);
        when(bookingRepository.findAll()).thenReturn(java.util.List.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(request, 1L));

        assertTrue(ex.getMessage().contains("already reserved"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should retrieve real-time seat status DTO with coach layouts for live polling")
    void testGetSeatStatus_Success() {
        LocalDate travelDate = LocalDate.now().plusDays(1);
        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(sampleSchedule));
        when(bookingRepository.findAll()).thenReturn(java.util.Collections.emptyList());

        com.trainbooking.it25103308.dto.SeatStatusResponseDto status = bookingService.getSeatStatus(10L, travelDate);

        assertNotNull(status);
        assertEquals(10L, status.getScheduleId());
        assertEquals(travelDate, status.getTravelDate());
        assertNotNull(status.getCoaches());
        assertFalse(status.getCoaches().isEmpty());
    }
}
