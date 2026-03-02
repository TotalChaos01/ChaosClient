package me.totalchaos01.chaosclient.notification;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rise-style notification manager — stacks notifications from bottom-right,
 * auto-removes expired ones, renders with smooth slide-in animation.
 */
public class NotificationManager {

    private final Deque<Notification> notifications = new ConcurrentLinkedDeque<>();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void add(String title, String message, NotificationType type, long durationMs) {
        notifications.add(new Notification(title, message, type, System.currentTimeMillis(), durationMs));
    }

    public void add(String message, NotificationType type) {
        // Duration scales with message length (Rise-style)
        long duration = Math.max(2000, mc.textRenderer.getWidth(message) * 30L);
        add("ChaosClient", message, type, duration);
    }

    public void info(String message) { add(message, NotificationType.INFO); }
    public void warning(String message) { add(message, NotificationType.WARNING); }
    public void error(String message) { add(message, NotificationType.ERROR); }
    public void success(String message) { add(message, NotificationType.SUCCESS); }

    /**
     * Render all active notifications — called from EventRender2D.
     */
    public void render(DrawContext ctx) {
        // Remove expired
        notifications.removeIf(Notification::isExpired);

        if (notifications.isEmpty()) return;

        int sh = mc.getWindow().getScaledHeight();
        int i = 0;

        for (Notification notification : notifications) {
            notification.targetY = sh - 50 - (35 * i);
            notification.render(ctx);
            i++;
        }
    }

    /**
     * Returns active (non-expired) notifications.
     */
    public Deque<Notification> getActiveNotifications() {
        notifications.removeIf(Notification::isExpired);
        return notifications;
    }
}
