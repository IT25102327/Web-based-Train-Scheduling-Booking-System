package com.trainbooking.it25100228;

import com.trainbooking.it25100228.dto.DashboardStatsDto;
import com.trainbooking.it25100228.service.DashboardService;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.repository.PaymentRepository;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25101520.repository.UserRepository;
import com.trainbooking.it25102327.repository.TrainRepository;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.trainbooking.it25100228.repository.BoardingLogRepository boardingLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Should aggregate correct metrics for admin executive dashboard")
    void getDashboardStats_Success() {
        when(bookingRepository.count()).thenReturn(15L);
        when(trainRepository.count()).thenReturn(5L);

        Payment p1 = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("1200.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .build();
        Payment p2 = Payment.builder()
                .id(2L)
                .amount(new BigDecimal("2300.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .build();
        Payment p3 = Payment.builder()
                .id(3L)
                .amount(new BigDecimal("990.00"))
                .status(Payment.PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        Booking b1 = Booking.builder().id(1L).status(Booking.BookingStatus.CONFIRMED).travelDate(LocalDate.now()).build();
        Booking b2 = Booking.builder().id(2L).status(Booking.BookingStatus.PENDING).travelDate(LocalDate.now().plusDays(1)).build();
        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2));

        User u1 = User.builder().id(1L).role(User.Role.PASSENGER).build();
        User u2 = User.builder().id(2L).role(User.Role.ADMIN).build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(boardingLogRepository.countByScanResult(any())).thenReturn(4L);

        DashboardStatsDto stats = dashboardService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(15L, stats.getTotalBookings());
        assertEquals(3500L, stats.getTotalRevenue());
        assertEquals(3500L, stats.getTodayRevenue());
        assertEquals(5L, stats.getActiveTrains());
        assertEquals(1L, stats.getPendingBookings());
        assertEquals(1L, stats.getTodayBookings());
        assertEquals(1L, stats.getTotalPassengers());
        assertEquals(4L, stats.getBoardedPassengers());
    }

    @Test
    @DisplayName("Should calculate daily revenue timelines accurately for charting")
    void getRevenueByDate_Success() {
        LocalDateTime now = LocalDateTime.now();
        Payment p1 = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("1500.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .paidAt(now)
                .build();
        Payment p2 = Payment.builder()
                .id(2L)
                .amount(new BigDecimal("2000.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .paidAt(now.minusDays(2))
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(p1, p2));

        Map<String, Object> result = dashboardService.getRevenueByDate(
                LocalDate.now().minusDays(5),
                LocalDate.now()
        );

        assertNotNull(result);
        assertTrue(result.containsKey("dailyRevenue"));
        @SuppressWarnings("unchecked")
        Map<String, Long> dailyRevenue = (Map<String, Long>) result.get("dailyRevenue");
        assertEquals(1500L, dailyRevenue.get(now.toLocalDate().toString()));
        assertEquals(2000L, dailyRevenue.get(now.minusDays(2).toLocalDate().toString()));
    }

    @Test
    @DisplayName("Should retrieve recent boarding logs sorted in descending order")
    void getRecentBoardingLogs_Success() {
        com.trainbooking.it25100228.model.BoardingLog log1 = com.trainbooking.it25100228.model.BoardingLog.builder()
                .id(101L)
                .scannedTicketNumber("TKT-001")
                .scanResult(com.trainbooking.it25100228.model.BoardingLog.ScanResult.VALID)
                .scannedAt(LocalDateTime.now())
                .build();
        com.trainbooking.it25100228.model.BoardingLog log2 = com.trainbooking.it25100228.model.BoardingLog.builder()
                .id(102L)
                .scannedTicketNumber("TKT-002")
                .scanResult(com.trainbooking.it25100228.model.BoardingLog.ScanResult.DUPLICATE)
                .scannedAt(LocalDateTime.now())
                .build();

        when(boardingLogRepository.findAll()).thenReturn(List.of(log1, log2));

        List<com.trainbooking.it25100228.model.BoardingLog> logs = dashboardService.getRecentBoardingLogs();

        assertNotNull(logs);
        assertEquals(2, logs.size());
        assertEquals(102L, logs.get(0).getId());
        assertEquals(101L, logs.get(1).getId());
    }

    @Test
    @DisplayName("Should generate daily settlement and reconciliation metrics")
    void getReconciliationReport_Success() {
        Payment p1 = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("3500.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .build();
        Payment p2 = Payment.builder()
                .id(2L)
                .amount(new BigDecimal("1200.00"))
                .status(Payment.PaymentStatus.FAILED)
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(p1, p2));

        Map<String, Object> report = dashboardService.getReconciliationReport();

        assertNotNull(report);
        assertEquals(2, report.get("totalTransactions"));
        assertEquals(1L, report.get("successfulTransactions"));
        assertEquals(1L, report.get("declinedTransactions"));
        assertEquals(new BigDecimal("3500.00"), report.get("grossSettlementAmount"));
        assertEquals("ONLINE / SYNCHRONIZED", report.get("gatewayStatus"));
    }
}
