package com.trainbooking.it25100228;

import com.trainbooking.it25100228.dto.ValidationResultDto;
import com.trainbooking.it25100228.service.TicketValidationService;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.repository.TicketRepository;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102327.model.Route;
import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25103308.model.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketValidationServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private com.trainbooking.it25100228.repository.BoardingLogRepository boardingLogRepository;

    @InjectMocks
    private TicketValidationService ticketValidationService;

    private Ticket sampleTicket;
    private Booking sampleBooking;
    private User samplePassenger;
    private Train sampleTrain;

    @BeforeEach
    void setUp() {
        samplePassenger = User.builder()
                .id(1L)
                .firstName("Kasun")
                .lastName("Perera")
                .build();

        sampleTrain = Train.builder()
                .id(1L)
                .trainName("Udarata Menike")
                .trainNumber("1015")
                .build();

        Route sampleRoute = Route.builder()
                .id(1L)
                .origin("Colombo Fort")
                .destination("Kandy")
                .build();

        Schedule sampleSchedule = Schedule.builder()
                .id(1L)
                .train(sampleTrain)
                .route(sampleRoute)
                .departureTime(LocalTime.of(5, 55))
                .arrivalTime(LocalTime.of(8, 45))
                .build();

        sampleBooking = Booking.builder()
                .id(100L)
                .passenger(samplePassenger)
                .passengerName("Kasun Perera")
                .schedule(sampleSchedule)
                .seatClass(Booking.SeatClass.FIRST)
                .seatNumbers("Car 01 / Seat 12")
                .numberOfSeats(1)
                .travelDate(LocalDate.now())
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("1800.00"))
                .build();

        sampleTicket = Ticket.builder()
                .id(1L)
                .ticketNumber("TKT-2026-10100")
                .booking(sampleBooking)
                .isBoarded(false)
                .qrCodeData("TKT-2026-10100")
                .build();
    }

    @Test
    @DisplayName("Should successfully validate ticket, mark as boarded, and authorize passenger entry")
    void validateAndBoardTicket_Success() {
        when(ticketRepository.findByTicketNumber("TKT-2026-10100")).thenReturn(Optional.of(sampleTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(sampleTicket);

        ValidationResultDto result = ticketValidationService.validateAndBoardTicket("TKT-2026-10100");

        assertNotNull(result);
        assertTrue(result.getValid());
        assertTrue(result.getMessage().contains("VALID TICKET"));
        assertTrue(sampleTicket.getIsBoarded());
        assertEquals("TKT-2026-10100", result.getTicketNumber());
        assertEquals("Kasun Perera", result.getPassengerName());
        assertEquals("Colombo Fort", result.getOrigin());
        assertEquals("Kandy", result.getDestination());
        assertEquals("Car 01 / Seat 12", result.getSeatNumbers());

        verify(ticketRepository, times(1)).save(sampleTicket);
        verify(boardingLogRepository, times(1)).save(any(com.trainbooking.it25100228.model.BoardingLog.class));
    }

    @Test
    @DisplayName("Should reject duplicate scan to prevent fraud if ticket is already marked as boarded")
    void validateAndBoardTicket_DuplicateScan_RejectsFraud() {
        sampleTicket.setIsBoarded(true);
        when(ticketRepository.findByTicketNumber("TKT-2026-10100")).thenReturn(Optional.of(sampleTicket));

        ValidationResultDto result = ticketValidationService.validateAndBoardTicket("TKT-2026-10100");

        assertNotNull(result);
        assertFalse(result.getValid());
        assertTrue(result.getMessage().contains("DUPLICATE SCAN ALERT"));
        verify(ticketRepository, never()).save(any());
        verify(boardingLogRepository, times(1)).save(any(com.trainbooking.it25100228.model.BoardingLog.class));
    }

    @Test
    @DisplayName("Should reject ticket if not found in database")
    void validateAndBoardTicket_NotFound() {
        when(ticketRepository.findByTicketNumber("INVALID-TKT")).thenReturn(Optional.empty());

        ValidationResultDto result = ticketValidationService.validateAndBoardTicket("INVALID-TKT");

        assertNotNull(result);
        assertFalse(result.getValid());
        assertTrue(result.getMessage().contains("No matching reservation found"));
        verify(boardingLogRepository, times(1)).save(any(com.trainbooking.it25100228.model.BoardingLog.class));
    }

    @Test
    @DisplayName("Should reject scan if QR code string is empty or null")
    void validateAndBoardTicket_EmptyInput() {
        ValidationResultDto resultNull = ticketValidationService.validateAndBoardTicket(null);
        assertNotNull(resultNull);
        assertFalse(resultNull.getValid());

        ValidationResultDto resultEmpty = ticketValidationService.validateAndBoardTicket("   ");
        assertNotNull(resultEmpty);
        assertFalse(resultEmpty.getValid());
        verify(boardingLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject ticket if associated booking is cancelled")
    void validateAndBoardTicket_CancelledBooking() {
        sampleBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(ticketRepository.findByTicketNumber("TKT-2026-10100")).thenReturn(Optional.of(sampleTicket));

        ValidationResultDto result = ticketValidationService.validateAndBoardTicket("TKT-2026-10100");

        assertNotNull(result);
        assertFalse(result.getValid());
        assertTrue(result.getMessage().contains("CANCELLED TICKET ALERT"));
        verify(ticketRepository, never()).save(any());
        verify(boardingLogRepository, times(1)).save(any(com.trainbooking.it25100228.model.BoardingLog.class));
    }

    @Test
    @DisplayName("Should retrieve recent tickets for gate display")
    void getRecentTickets_Success() {
        when(ticketRepository.findTop10ByOrderByIdDesc()).thenReturn(List.of(sampleTicket));

        List<Ticket> result = ticketValidationService.getRecentTickets();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TKT-2026-10100", result.get(0).getTicketNumber());
        verify(ticketRepository, times(1)).findTop10ByOrderByIdDesc();
    }

    @Test
    @DisplayName("Should reset ticket boarded status to unboarded for testing")
    void resetBoardedStatus_Success() {
        sampleTicket.setIsBoarded(true);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(sampleTicket));
        when(ticketRepository.save(sampleTicket)).thenReturn(sampleTicket);

        boolean reset = ticketValidationService.resetBoardedStatus(1L);

        assertTrue(reset);
        assertFalse(sampleTicket.getIsBoarded());
        verify(ticketRepository, times(1)).save(sampleTicket);
    }
}
