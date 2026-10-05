package com.trainbooking.it25102925.service;

import com.trainbooking.exception.ResourceNotFoundException;
import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102327.model.Train;
import com.trainbooking.it25102327.repository.TrainRepository;
import com.trainbooking.it25102925.model.Notification;
import com.trainbooking.it25102925.repository.NotificationRepository;
import com.trainbooking.it25103308.model.Booking;
import com.trainbooking.it25103308.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for creating, dispatching, and managing passenger notifications regarding delays and schedule changes.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final TrainRepository trainRepository;
    private final BookingRepository bookingRepository;
    private final com.trainbooking.it25102925.repository.RebookingTokenRepository rebookingTokenRepository;
    private final com.trainbooking.it25102327.repository.ScheduleRepository scheduleRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.trainbooking.it25102925.channel.NotificationChannelFactory channelFactory;

    /**
     * Finds all ticket holders for the affected train and dispatches alerts via Email and in-app notifications.
     * When a train is CANCELLED, generates a secure 1-Click Rebooking link for free seat transfer.
     *
     * @param trainId train ID
     * @param newStatus updated status (e.g. DELAYED by 30 mins, CANCELLED)
     */
    @Transactional
    public void notifyAffectedPassengers(Long trainId, String newStatus) {
        log.info("Notifying affected passengers for train ID {} with status update: {}", trainId, newStatus);
        Train train = trainRepository.findById(trainId).orElse(null);
        String trainName = (train != null) ? train.getTrainName() : "Train #" + trainId;
        String trainNum = (train != null) ? train.getTrainNumber() : String.valueOf(trainId);

        boolean isCancelled = newStatus != null && newStatus.toUpperCase().contains("CANCEL");

        List<Booking> activeBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getSchedule() != null && b.getSchedule().getTrain() != null
                        && b.getSchedule().getTrain().getId().equals(trainId))
                .toList();

        for (Booking booking : activeBookings) {
            User passenger = booking.getPassenger();
            if (passenger == null) continue;

            String subject = "Sri Lanka Railways Alert: " + trainName + " (" + trainNum + ") " + newStatus;
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Dear ").append(passenger.getFirstName())
                    .append(", please be advised that train service ").append(trainName)
                    .append(" has received a schedule status update: ").append(newStatus).append(".");

            if (isCancelled) {
                // Generate 1-Click Free Rebooking Token
                String tokenStr = java.util.UUID.randomUUID().toString();
                com.trainbooking.it25102925.model.RebookingToken rebookToken = com.trainbooking.it25102925.model.RebookingToken.builder()
                        .booking(booking)
                        .token(tokenStr)
                        .isRedeemed(false)
                        .expiresAt(java.time.LocalDateTime.now().plusHours(24))
                        .build();
                rebookingTokenRepository.save(rebookToken);

                messageBuilder.append("\n\nYour train has been cancelled. Click here to claim your complimentary 1-Click Rebooking on the next available train: ")
                        .append("/rebooking/claim/").append(tokenStr);
            } else {
                messageBuilder.append(" Please check your travel schedule or visit our departure board for real-time tracking.");
            }

            String message = messageBuilder.toString();

            Notification notification = Notification.builder()
                    .recipient(passenger)
                    .subject(subject)
                    .message(message)
                    .type(Notification.NotificationType.IN_APP)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);

            // Multi-channel Email Dispatch (respecting user preference)
            boolean shouldEmail = (passenger.getNotifyByEmail() == null || passenger.getNotifyByEmail());
            if (shouldEmail && passenger.getEmail() != null && !passenger.getEmail().isBlank()) {
                sendEmail(passenger.getEmail(), subject, message);
            }

            // Multi-channel SMS Dispatch Simulation (respecting user preference)
            boolean shouldSms = (passenger.getNotifyBySms() == null || passenger.getNotifyBySms());
            if (shouldSms && passenger.getPhone() != null && !passenger.getPhone().isBlank()) {
                log.info("[SMS GATEWAY SIMULATION] Transmitted SMS alert to {}: {}", passenger.getPhone(), subject);
            }
        }

        log.info("Dispatched {} multi-channel notifications for train ID {}", activeBookings.size(), trainId);
    }

    /**
     * Asynchronously dispatches passenger delay and cancellation alerts in background thread pool.
     */
    @org.springframework.scheduling.annotation.Async
    public void notifyAffectedPassengersAsync(Long trainId, String newStatus) {
        notifyAffectedPassengers(trainId, newStatus);
    }

    /**
     * Redeems a cancellation rebooking token and transfers the passenger's reservation
     * to a new schedule at zero additional charge.
     *
     * @param tokenStr secure rebooking token
     * @param newScheduleId the replacement schedule ID
     * @return updated {@link Booking}
     */
    @Transactional
    public Booking claimRebooking(String tokenStr, Long newScheduleId) {
        log.info("Redeeming rebooking token: {} for new schedule ID: {}", tokenStr, newScheduleId);
        com.trainbooking.it25102925.model.RebookingToken token = rebookingTokenRepository.findByTokenAndIsRedeemedFalse(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Rebooking token is invalid, already redeemed, or expired."));

        if (token.isExpired()) {
            throw new IllegalStateException("Rebooking claim period has expired.");
        }

        com.trainbooking.it25102327.model.Schedule newSchedule = scheduleRepository.findById(newScheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Replacement schedule not found with ID: " + newScheduleId));

        Booking booking = token.getBooking();
        booking.setSchedule(newSchedule);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        token.setIsRedeemed(true);
        rebookingTokenRepository.save(token);

        log.info("Successfully rebooked booking ID: {} onto schedule ID: {} without additional cost", booking.getId(), newSchedule.getId());
        return booking;
    }

    /**
     * Sends an email notification.
     *
     * @param to recipient email
     * @param subject email subject
     * @param body email body text
     */
    public void sendEmail(String to, String subject, String body) {
        log.info("Dispatching email notification to: {}", to);
        emailService.sendSimpleEmail(to, subject, body);
    }

    /**
     * Retrieves all notifications for a specific user.
     *
     * @param userId user ID
     * @return list of {@link Notification} records
     */
     public List<Notification> getNotificationsForUser(Long userId) {
        log.info("Fetching notifications for user ID: {}", userId);
        return notificationRepository.findByRecipientIdOrderBySentAtDesc(userId);
    }

    /**
     * Marks all notifications as read for a specific user.
     *
     * @param userId user ID
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user ID: {}", userId);
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderBySentAtDesc(userId);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }

    /**
     * Sends automated email and creates an in-app notification confirming booking cancellation and refund details.
     *
     * @param booking cancelled booking
     * @param refund processed refund (if applicable)
     */
    @Transactional
    public void sendCancellationNotification(Booking booking, com.trainbooking.it25100977.model.Refund refund) {
        if (booking == null || booking.getPassenger() == null) {
            return;
        }

        User passenger = booking.getPassenger();
        String ticketNo = booking.getTicketNumber();
        String trainName = booking.getTrainName();
        String route = booking.getOrigin() + " to " + booking.getDestination();
        String refundText = (refund != null)
                ? "Refund Amount: LKR " + String.format("%,.2f", refund.getRefundAmount()) + " (Ref: " + refund.getRefundTransactionRef() + ", " + refund.getRefundPercentage() + "% refunded)"
                : "No payment was charged.";

        String subject = "Reservation Cancelled & Refund Processed: " + ticketNo + " (" + trainName + ")";
        String message = String.format(
                "Your reservation for %s (%s) on %s has been cancelled successfully.%n" +
                "Seats Released: %s%n" +
                "%s%n" +
                "Thank you for choosing Sri Lanka Railways.",
                trainName, route, booking.getTravelDate(), booking.getSeatNumbers(), refundText
        );

        Notification notification = Notification.builder()
                .recipient(passenger)
                .subject(subject)
                .message(message)
                .type(Notification.NotificationType.IN_APP)
                .isRead(false)
                .sentAt(java.time.LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        if (passenger.getEmail() != null && !passenger.getEmail().isBlank()) {
            emailService.sendSimpleEmail(passenger.getEmail(), subject, message);
        }

        log.info("Dispatched cancellation & refund notification to passenger: {}", passenger.getEmail());
    }
}
