package com.trainbooking.it25102925.channel;

import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102925.model.Notification;
import com.trainbooking.it25102925.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-App notification channel that persists alerts directly to the database.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification.NotificationType getChannelType() {
        return Notification.NotificationType.IN_APP;
    }

    @Override
    public void sendNotification(User recipient, String subject, String message) {
        if (recipient == null) return;
        log.info("Dispatching In-App notification to user ID: {}", recipient.getId());

        Notification notification = Notification.builder()
                .recipient(recipient)
                .subject(subject)
                .message(message)
                .type(Notification.NotificationType.IN_APP)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }
}
