package com.trainbooking.it25102925.channel;

import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102925.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS notification channel simulating transmission through telecommunications gateway.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Component
public class SmsNotificationChannel implements NotificationChannel {

    @Override
    public Notification.NotificationType getChannelType() {
        return Notification.NotificationType.SMS;
    }

    @Override
    public void sendNotification(User recipient, String subject, String message) {
        if (recipient == null || recipient.getPhone() == null || recipient.getPhone().isBlank()) {
            return;
        }

        boolean shouldSms = (recipient.getNotifyBySms() == null || recipient.getNotifyBySms());
        if (!shouldSms) {
            log.debug("User {} opted out of SMS alerts, skipping.", recipient.getId());
            return;
        }

        log.info("[SMS GATEWAY SIMULATION] Transmitted SMS alert to {}: {}", recipient.getPhone(), subject);
    }
}
