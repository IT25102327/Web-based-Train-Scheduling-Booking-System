package com.trainbooking.it25102925.channel;

import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102925.model.Notification;
import com.trainbooking.it25102925.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email notification channel delivering alerts to passenger email inboxes.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailService emailService;

    @Override
    public Notification.NotificationType getChannelType() {
        return Notification.NotificationType.EMAIL;
    }

    @Override
    public void sendNotification(User recipient, String subject, String message) {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return;
        }

        boolean shouldEmail = (recipient.getNotifyByEmail() == null || recipient.getNotifyByEmail());
        if (!shouldEmail) {
            log.debug("User {} opted out of email alerts, skipping.", recipient.getId());
            return;
        }

        log.info("Dispatching email notification to: {}", recipient.getEmail());
        emailService.sendSimpleEmail(recipient.getEmail(), subject, message);
    }
}
