package com.trainbooking.it25100977;

import com.trainbooking.it25100977.dto.PaymentRequest;
import com.trainbooking.it25100977.model.Payment;
import com.trainbooking.it25100977.model.Ticket;
import com.trainbooking.it25100977.repository.PaymentRepository;
import com.trainbooking.it25100977.repository.TicketRepository;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import com.trainbooking.it25100977.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private com.trainbooking.it25102925.service.EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private Booking sampleBooking;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        sampleBooking = Booking.builder()
                .id(1L)
                .totalAmount(new BigDecimal("1500.00"))
                .numberOfSeats(1)
                .status(Booking.BookingStatus.PENDING)
                .build();

        samplePayment = Payment.builder()
                .id(10L)
                .booking(sampleBooking)
                .amount(new BigDecimal("1500.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionRef("TXN-12345678")
                .build();
    }

    @Test
    @DisplayName("Should successfully process payment and confirm booking")
    void testProcessPayment_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        PaymentRequest request = new PaymentRequest();
        Payment result = paymentService.processPayment(1L, request);

        assertNotNull(result);
        assertEquals(Payment.PaymentStatus.COMPLETED, result.getStatus());
        assertEquals(Booking.BookingStatus.CONFIRMED, sampleBooking.getStatus());
        assertNotNull(result.getTransactionRef());
        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("Should simulate payment decline and record failed payment when card ends with 0000")
    void testProcessPayment_DeclineSimulated() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        PaymentRequest request = new PaymentRequest();
        request.setCardNumber("4111222233330000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPayment(1L, request));

        assertTrue(ex.getMessage().contains("declined"));
        assertEquals(Booking.BookingStatus.PENDING, sampleBooking.getStatus()); // remains pending for retry
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should generate e-ticket with QR code data")
    void testGenerateTicket_Success() {
        when(ticketRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(samplePayment));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
            Ticket t = i.getArgument(0);
            t.setId(50L);
            return t;
        });

        Ticket ticket = paymentService.generateTicket(1L);

        assertNotNull(ticket);
        assertEquals(50L, ticket.getId());
        assertEquals("TKT-2026-10001", ticket.getTicketNumber());
        assertEquals("TKT-2026-10001", ticket.getQrCodeData());
        assertFalse(ticket.getIsBoarded());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should generate valid QR code PNG bytes")
    void testGenerateQrCodeImage() {
        Ticket ticket = Ticket.builder()
                .id(50L)
                .ticketNumber("TKT-2026-10001")
                .build();
        when(ticketRepository.findById(50L)).thenReturn(Optional.of(ticket));

        byte[] qrBytes = paymentService.generateQrCodeImage(50L);

        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 0);
    }

    @Test
    @DisplayName("Should generate valid PDF stream bytes")
    void testGenerateTicketPdf() {
        Ticket ticket = Ticket.builder()
                .id(50L)
                .ticketNumber("TKT-2026-10001")
                .booking(sampleBooking)
                .payment(samplePayment)
                .build();
        when(ticketRepository.findById(50L)).thenReturn(Optional.of(ticket));

        byte[] pdfBytes = paymentService.generateTicketPdf(50L);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    @DisplayName("Should generate valid tax invoice receipt PDF stream bytes")
    void testGenerateInvoicePdf() {
        Ticket ticket = Ticket.builder()
                .id(50L)
                .ticketNumber("TKT-2026-10001")
                .booking(sampleBooking)
                .payment(samplePayment)
                .build();
        when(ticketRepository.findById(50L)).thenReturn(Optional.of(ticket));

        byte[] invoiceBytes = paymentService.generateInvoicePdf(50L);

        assertNotNull(invoiceBytes);
        assertTrue(invoiceBytes.length > 0);
        String content = new String(invoiceBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("Tax Invoice"));
    }
}
