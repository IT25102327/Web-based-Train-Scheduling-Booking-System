package com.trainbooking.it25102925;

import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102327.model.Schedule;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.TrainRepository;
import com.trainbooking.it25102925.model.Notification;
import com.trainbooking.it25102925.repository.NotificationRepository;
import com.trainbooking.it25102925.service.EmailService;
import com.trainbooking.it25102925.service.NotificationService;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private com.trainbooking.it25102925.repository.RebookingTokenRepository rebookingTokenRepository;

    @Mock
    private com.trainbooking.it25102327.repository.ScheduleRepository scheduleRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User samplePassenger;
    private Train sampleTrain;
    private Schedule sampleSchedule;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        samplePassenger = User.builder()
                .id(10L)
                .firstName("Kasun")
                .lastName("Perera")
                .email("passenger@trainbooking.lk")
                .build();

        sampleTrain = Train.builder()
                .id(1L)
                .trainNumber("1015")
                .trainName("Udarata Menike")
                .build();

        sampleSchedule = Schedule.builder()
                .id(100L)
                .train(sampleTrain)
                .build();

        sampleBooking = Booking.builder()
                .id(501L)
                .passenger(samplePassenger)
                .schedule(sampleSchedule)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
    }

    @Test
    @DisplayName("Should dispatch notification and email to all affected passengers when train is delayed")
    void notifyAffectedPassengers_Success() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));
        when(bookingRepository.findAll()).thenReturn(List.of(sampleBooking));

        notificationService.notifyAffectedPassengers(1L, "DELAYED by 45 mins");

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notifCaptor.capture());

        Notification captured = notifCaptor.getValue();
        assertEquals(samplePassenger, captured.getRecipient());
        assertTrue(captured.getSubject().contains("Udarata Menike"));
        assertTrue(captured.getMessage().contains("DELAYED by 45 mins"));
        assertFalse(captured.getIsRead());

        verify(emailService, times(1)).sendSimpleEmail(
                eq("passenger@trainbooking.lk"),
                contains("Udarata Menike"),
                contains("DELAYED by 45 mins")
        );
    }

    @Test
    @DisplayName("Should handle train status update gracefully when no bookings exist")
    void notifyAffectedPassengers_NoBookings() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));
        when(bookingRepository.findAll()).thenReturn(List.of());

        notificationService.notifyAffectedPassengers(1L, "CANCELLED");

        verify(notificationRepository, never()).save(any());
        verify(emailService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should invoke emailService directly when sendEmail is called")
    void sendEmail_Success() {
        notificationService.sendEmail("test@trainbooking.lk", "Urgent Alert", "Train is delayed");

        verify(emailService, times(1)).sendSimpleEmail(
                "test@trainbooking.lk",
                "Urgent Alert",
                "Train is delayed"
        );
    }

    @Test
    @DisplayName("Should return list of notifications for user ordered by sentAt descending")
    void getNotificationsForUser_Success() {
        Notification notif1 = Notification.builder().id(1L).recipient(samplePassenger).subject("Alert 1").build();
        Notification notif2 = Notification.builder().id(2L).recipient(samplePassenger).subject("Alert 2").build();

        when(notificationRepository.findByRecipientIdOrderBySentAtDesc(10L)).thenReturn(List.of(notif2, notif1));

        List<Notification> results = notificationService.getNotificationsForUser(10L);

        assertEquals(2, results.size());
        assertEquals("Alert 2", results.get(0).getSubject());
        verify(notificationRepository, times(1)).findByRecipientIdOrderBySentAtDesc(10L);
    }

    @Test
    @DisplayName("Should generate rebooking token and embed claim URL when train is cancelled")
    void notifyAffectedPassengers_CancelledGeneratesRebookingToken() {
        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));
        when(bookingRepository.findAll()).thenReturn(List.of(sampleBooking));

        notificationService.notifyAffectedPassengers(1L, "CANCELLED due to severe weather");

        verify(rebookingTokenRepository, times(1)).save(any(com.trainbooking.it25102925.model.RebookingToken.class));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("/rebooking/claim/"));
    }

    @Test
    @DisplayName("Should transfer schedule at zero additional charge when rebooking token is redeemed")
    void claimRebooking_Success() {
        com.trainbooking.it25102925.model.RebookingToken token = com.trainbooking.it25102925.model.RebookingToken.builder()
                .token("test-secure-token-123")
                .booking(sampleBooking)
                .isRedeemed(false)
                .expiresAt(java.time.LocalDateTime.now().plusHours(12))
                .build();

        Schedule newSchedule = Schedule.builder().id(200L).build();

        when(rebookingTokenRepository.findByTokenAndIsRedeemedFalse("test-secure-token-123")).thenReturn(Optional.of(token));
        when(scheduleRepository.findById(200L)).thenReturn(Optional.of(newSchedule));

        Booking updated = notificationService.claimRebooking("test-secure-token-123", 200L);

        assertNotNull(updated);
        assertEquals(200L, updated.getSchedule().getId());
        assertTrue(token.getIsRedeemed());
        verify(bookingRepository, times(1)).save(sampleBooking);
        verify(rebookingTokenRepository, times(1)).save(token);
    }

    @Test
    @DisplayName("Should respect passenger notification preferences during dispatch")
    void notifyAffectedPassengers_RespectsPreferences() {
        samplePassenger.setNotifyByEmail(false);
        samplePassenger.setNotifyBySms(true);
        samplePassenger.setPhone("+94779998877");

        when(trainRepository.findById(1L)).thenReturn(Optional.of(sampleTrain));
        when(bookingRepository.findAll()).thenReturn(List.of(sampleBooking));

        notificationService.notifyAffectedPassengers(1L, "DELAYED by 20 mins");

        verify(emailService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
