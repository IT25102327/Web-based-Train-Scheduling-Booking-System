package com.trainbooking.it25102925.channel;

import com.trainbooking.it25101520.model.User;
import com.trainbooking.it25102925.model.Notification;

/**
 * Channel interface for transmitting notification payloads via specific communication media.
 * Part of the Factory Method and Strategy Pattern design for multi-channel messaging.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
public interface NotificationChannel {

    /**
     * Transmits the notification to the target user.
     *
     * @param recipient targeted passenger or staff member
     * @param subject notification header / subject line
     * @param message notification body content
     */
    void sendNotification(User recipient, String subject, String message);

    /**
     * Identifies the notification channel type.
     *
     * @return {@link Notification.NotificationType}
     */
    Notification.NotificationType getChannelType();
}
