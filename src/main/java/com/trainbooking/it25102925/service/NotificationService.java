package com.trainbooking.it25102925.service;
import com.trainbooking.it25102925.model.*;
import com.trainbooking.it25102925.dto.*;
import com.trainbooking.it25102925.repository.*;

import com.trainbooking.it25102925.model.Notification;
import com.trainbooking.it25102925.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    /**
     * Finds all ticket holders for the affected train and dispatches alerts via Email and in-app notifications.
     *
     * @param trainId train ID
     * @param newStatus updated status (e.g. DELAYED by 30 mins, CANCELLED)
     */
    public void notifyAffectedPassengers(Long trainId, String newStatus) {
        log.info("Notifying affected passengers for train ID {} with status update: {}", trainId, newStatus);
        // TODO: Query active bookings for this train, generate Notification entities, and trigger email alerts
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
}
