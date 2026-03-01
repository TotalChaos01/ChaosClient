package me.totalchaos01.chaosclient.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages on-screen notifications for the client.
 */
public class NotificationManager {

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void add(String title, String message, NotificationType type, long durationMs) {
        notifications.add(new Notification(title, message, type, System.currentTimeMillis(), durationMs));
    }

    public void add(String message, NotificationType type) {
        add("ChaosClient", message, type, 3000);
    }

    public void info(String message) {
        add(message, NotificationType.INFO);
    }

    public void warning(String message) {
        add(message, NotificationType.WARNING);
    }

    public void error(String message) {
        add(message, NotificationType.ERROR);
    }

    public void success(String message) {
        add(message, NotificationType.SUCCESS);
    }

    /**
     * Returns active notifications and cleans up expired ones.
     */
    public List<Notification> getActiveNotifications() {
        long now = System.currentTimeMillis();
        notifications.removeIf(n -> now - n.createdAt() > n.durationMs());
        return notifications;
    }
}
