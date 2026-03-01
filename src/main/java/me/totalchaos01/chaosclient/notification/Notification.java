package me.totalchaos01.chaosclient.notification;

public record Notification(
        String title,
        String message,
        NotificationType type,
        long createdAt,
        long durationMs
) {
}
