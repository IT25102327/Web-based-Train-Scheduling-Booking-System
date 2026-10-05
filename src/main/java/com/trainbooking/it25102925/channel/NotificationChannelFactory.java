package com.trainbooking.it25102925.channel;

import com.trainbooking.it25102925.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class providing notification channels by channel type.
 *
 * @author SLIIT Software Engineering Team (IT25102925)
 * @version 1.0.0
 */
@Slf4j
@Component
public class NotificationChannelFactory {

    private final Map<Notification.NotificationType, NotificationChannel> channels =
            new EnumMap<>(Notification.NotificationType.class);

    public NotificationChannelFactory(List<NotificationChannel> channelList) {
        for (NotificationChannel channel : channelList) {
            channels.put(channel.getChannelType(), channel);
            log.info("Registered NotificationChannel: {}", channel.getChannelType());
        }
    }

    /**
     * Resolves notification channel for the given type.
     *
     * @param type {@link Notification.NotificationType}
     * @return {@link NotificationChannel}
     */
    public NotificationChannel getChannel(Notification.NotificationType type) {
        return channels.get(type);
    }

    /**
     * Returns all registered notification channels.
     *
     * @return collection of channels
     */
    public List<NotificationChannel> getAllChannels() {
        return List.copyOf(channels.values());
    }
}
