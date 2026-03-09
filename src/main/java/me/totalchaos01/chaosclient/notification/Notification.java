package me.totalchaos01.chaosclient.notification;

import me.totalchaos01.chaosclient.font.ChaosFont;
import me.totalchaos01.chaosclient.util.render.Animate;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * Rise-style notification — slides in from the right, has a progress bar that depletes,
 * auto-dismisses after duration. Multiple notifications stack vertically.
 */
public class Notification {

    private final String title;
    private final String message;
    private final NotificationType type;
    private final long createdAt;
    private final long durationMs;

    // Animation
    private float xVisual;
    private float yVisual;
    public float targetY;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public Notification(String title, String message, NotificationType type, long createdAt, long durationMs) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.durationMs = durationMs;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        this.xVisual = sw; // start offscreen right
        this.yVisual = sh - 50;
        this.targetY = sh - 50;
    }

    public void render(DrawContext ctx) {
        int sw = mc.getWindow().getScaledWidth();
        int textW = Math.max(ChaosFont.getWidth(message), ChaosFont.getWidth(title));
        float targetX = sw - textW - 20;

        float elapsed = System.currentTimeMillis() - createdAt;
        float progress = elapsed / durationMs;

        // Slide out when 90%+ through
        if (progress > 0.9f) {
            targetX = sw + 10;
        }

        // Smooth lerp
        xVisual = (float) Animate.lerp(xVisual, targetX, 0.15);
        yVisual = (float) Animate.lerp(yVisual, targetY, 0.15);

        // Theme color
        Color themeColor = ThemeUtil.getThemeColor(ThemeType.GENERAL);
        int themeCARGB = ColorUtil.toARGB(themeColor);

        // Background rounded rect (left corners only)
        float boxWidth = sw - xVisual;
        RenderUtil.roundedRectSimple(ctx,
                (int) xVisual, (int) yVisual - 3,
                (int) boxWidth + 2, 32,
                4, 0xAA000000);

        // Progress bar at bottom
        float barWidth = (1.0f - progress) * (textW + 12);
        ctx.fill((int) xVisual, (int) yVisual + 26, (int) (xVisual + barWidth), (int) yVisual + 28, themeCARGB);

        // Title (brighter)
        Color bright = new Color(
                Math.min(themeColor.getRed() + 30, 255),
                Math.min(themeColor.getGreen() + 40, 255),
                Math.min(themeColor.getBlue() + 20, 255)
        );
        ChaosFont.drawWithShadow(ctx, title, (int) xVisual + 4, (int) yVisual + 1, ColorUtil.toARGB(bright));

        // Message
        ChaosFont.drawWithShadow(ctx, message, (int) xVisual + 4, (int) yVisual + 12, themeCARGB);
    }

    // ─── Getters ──────────────────────────────────────────────

    public String title() { return title; }
    public String message() { return message; }
    public NotificationType type() { return type; }
    public long createdAt() { return createdAt; }
    public long durationMs() { return durationMs; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > durationMs;
    }
}
