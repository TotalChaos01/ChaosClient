package me.totalchaos01.chaosclient.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Advanced rendering utility for ChaosClient.
 * Smooth rounded rects, gradient rounded rects, glow, shadow,
 * outlined rects, circles, toggle switches, progress bars.
 */
public final class RenderUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private RenderUtil() {}

    // ─── Basic Rectangles ─────────────────────────────────────

    public static void rect(DrawContext ctx, double x, double y, double width, double height, int color) {
        ctx.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    public static void rectAbs(DrawContext ctx, double x1, double y1, double x2, double y2, int color) {
        ctx.fill((int) x1, (int) y1, (int) x2, (int) y2, color);
    }

    // ─── High-Quality Rounded Rectangles ──────────────────────

    public static void roundedRect(DrawContext ctx, double x, double y, double width, double height,
                                   double radius, int color) {
        roundedRectSimple(ctx, (int) x, (int) y, (int) width, (int) height, (int) radius, color);
    }

    public static void roundedRectSimple(DrawContext ctx, int x, int y, int width, int height,
                                         int radius, int color) {
        if (width <= 0 || height <= 0) return;
        if (((color >> 24) & 0xFF) == 0) return;
        if (radius <= 0) { ctx.fill(x, y, x + width, y + height, color); return; }
        radius = Math.min(radius, Math.min(width, height) / 2);

        ctx.fill(x + radius, y, x + width - radius, y + height, color);
        ctx.fill(x, y + radius, x + radius, y + height - radius, color);
        ctx.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i <= radius; i++) {
            int offset = (int) (radius - Math.sqrt((double) radius * radius - (double) (radius - i) * (radius - i)));
            ctx.fill(x + offset, y + i, x + radius, y + i + 1, color);
            ctx.fill(x + width - radius, y + i, x + width - offset, y + i + 1, color);
            ctx.fill(x + offset, y + height - i - 1, x + radius, y + height - i, color);
            ctx.fill(x + width - radius, y + height - i - 1, x + width - offset, y + height - i, color);
        }
    }

    /**
     * Rounded rectangle with horizontal gradient (left → right).
     */
    public static void roundedRectGradientH(DrawContext ctx, int x, int y, int width, int height,
                                            int radius, int leftColor, int rightColor) {
        if (width <= 0 || height <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);
        for (int col = 0; col < width; col++) {
            float t = (float) col / Math.max(1, width - 1);
            int color = ColorUtil.interpolateColor(leftColor, rightColor, t);
            int cx = x + col;
            if (col < radius || col >= width - radius) {
                for (int row = 0; row < height; row++) {
                    if (isInsideRoundedRect(col, row, width, height, radius)) {
                        ctx.fill(cx, y + row, cx + 1, y + row + 1, color);
                    }
                }
            } else {
                ctx.fill(cx, y, cx + 1, y + height, color);
            }
        }
    }

    /**
     * Rounded rectangle with vertical gradient (top → bottom).
     */
    public static void roundedRectGradientV(DrawContext ctx, int x, int y, int width, int height,
                                            int radius, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);
        for (int row = 0; row < height; row++) {
            float t = (float) row / Math.max(1, height - 1);
            int color = ColorUtil.interpolateColor(topColor, bottomColor, t);
            if (row < radius || row >= height - radius) {
                for (int col = 0; col < width; col++) {
                    if (isInsideRoundedRect(col, row, width, height, radius)) {
                        ctx.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                    }
                }
            } else {
                ctx.fill(x, y + row, x + width, y + row + 1, color);
            }
        }
    }

    private static boolean isInsideRoundedRect(int px, int py, int w, int h, int r) {
        if (px >= r && px < w - r) return true;
        if (py >= r && py < h - r) return true;
        int cx, cy;
        if (px < r && py < r) { cx = r; cy = r; }
        else if (px >= w - r && py < r) { cx = w - r; cy = r; }
        else if (px < r && py >= h - r) { cx = r; cy = h - r; }
        else if (px >= w - r && py >= h - r) { cx = w - r; cy = h - r; }
        else return true;
        return Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy)) <= r;
    }

    /**
     * Rounded rectangle outline.
     */
    public static void roundedRectOutline(DrawContext ctx, int x, int y, int width, int height,
                                          int radius, int thickness, int color) {
        if (width <= 0 || height <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);
        for (int t = 0; t < thickness; t++) {
            ctx.fill(x + radius, y + t, x + width - radius, y + t + 1, color);
            ctx.fill(x + radius, y + height - 1 - t, x + width - radius, y + height - t, color);
            ctx.fill(x + t, y + radius, x + t + 1, y + height - radius, color);
            ctx.fill(x + width - 1 - t, y + radius, x + width - t, y + height - radius, color);
        }
        for (int i = 0; i <= radius; i++) {
            int offset = (int) (radius - Math.sqrt((double) radius * radius - (double) (radius - i) * (radius - i)));
            for (int t = 0; t < thickness; t++) {
                ctx.fill(x + offset + t, y + i, x + offset + t + 1, y + i + 1, color);
                ctx.fill(x + width - offset - 1 - t, y + i, x + width - offset - t, y + i + 1, color);
                ctx.fill(x + offset + t, y + height - i - 1, x + offset + t + 1, y + height - i, color);
                ctx.fill(x + width - offset - 1 - t, y + height - i - 1, x + width - offset - t, y + height - i, color);
            }
        }
    }

    // ─── Gradient Rectangles ──────────────────────────────────

    public static void gradientRect(DrawContext ctx, double x, double y, double width, double height,
                                    int topColor, int bottomColor) {
        ctx.fillGradient((int) x, (int) y, (int) (x + width), (int) (y + height), topColor, bottomColor);
    }

    public static void horizontalGradient(DrawContext ctx, double x, double y, double width, double height,
                                          int leftColor, int rightColor) {
        int strips = Math.max(1, (int) width);
        for (int i = 0; i < strips; i++) {
            float t = (float) i / strips;
            int color = ColorUtil.interpolateColor(leftColor, rightColor, t);
            ctx.fill((int) (x + i), (int) y, (int) (x + i + 1), (int) (y + height), color);
        }
    }

    // ─── Circles ──────────────────────────────────────────────

    public static void circle(DrawContext ctx, double centerX, double centerY, double radius, int color) {
        int r = (int) radius;
        int cx = (int) centerX, cy = (int) centerY;
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            ctx.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
        }
    }

    public static void circleOutline(DrawContext ctx, double centerX, double centerY, double radius,
                                     int thickness, int color) {
        int r = (int) radius;
        int cx = (int) centerX, cy = (int) centerY;
        for (int dy = -r; dy <= r; dy++) {
            int dxOuter = (int) Math.sqrt(r * r - dy * dy);
            int rInner = r - thickness;
            int dxInner = rInner > 0 && dy * dy <= rInner * rInner
                    ? (int) Math.sqrt(rInner * rInner - dy * dy) : 0;
            ctx.fill(cx - dxOuter, cy + dy, cx - dxInner, cy + dy + 1, color);
            ctx.fill(cx + dxInner, cy + dy, cx + dxOuter, cy + dy + 1, color);
        }
    }

    // ─── Outline Rectangles ───────────────────────────────────

    public static void rectOutline(DrawContext ctx, double x, double y, double width, double height,
                                   double thickness, int color) {
        int t = Math.max(1, (int) thickness);
        rect(ctx, x, y, width, t, color);
        rect(ctx, x, y + height - t, width, t, color);
        rect(ctx, x, y, t, height, color);
        rect(ctx, x + width - t, y, t, height, color);
    }

    // ─── Advanced Shadow (Rise-style) ─────────────────────────

    public static void shadow(DrawContext ctx, double x, double y, double width, double height,
                              double radius, int shadowColor) {
        shadow(ctx, x, y, width, height, radius, shadowColor, 12, 2.0);
    }

    public static void shadow(DrawContext ctx, double x, double y, double width, double height,
                              double radius, int shadowColor, int layers, double spread) {
        int baseAlpha = (shadowColor >> 24) & 0xFF;
        for (int i = layers; i > 0; i--) {
            int expand = (int) (i * spread);
            double progress = (double) i / layers;
            int alpha = (int) (baseAlpha * (1.0 - progress) * (1.0 - progress) * 0.35);
            if (alpha <= 0) continue;
            int color = (alpha << 24) | (shadowColor & 0x00FFFFFF);
            roundedRectSimple(ctx, (int) (x - expand), (int) (y - expand),
                    (int) (width + expand * 2), (int) (height + expand * 2),
                    (int) (radius + expand), color);
        }
    }

    /**
     * Colored glow behind active elements.
     */
    public static void glow(DrawContext ctx, double x, double y, double width, double height,
                            double radius, Color glowColor, int intensity) {
        for (int i = intensity; i > 0; i--) {
            int expand = (int) (i * 1.8);
            double progress = (double) i / intensity;
            int alpha = (int) (glowColor.getAlpha() * (1.0 - progress * progress) * 0.2);
            if (alpha <= 0) continue;
            int color = (alpha << 24) | (glowColor.getRGB() & 0x00FFFFFF);
            roundedRectSimple(ctx, (int) (x - expand), (int) (y - expand),
                    (int) (width + expand * 2), (int) (height + expand * 2),
                    (int) (radius + expand), color);
        }
    }

    // ─── Toggle Switch (iOS-style) ────────────────────────────

    public static void toggleSwitch(DrawContext ctx, int x, int y, int width, int height,
                                    float progress, int offColor, int onColor) {
        int trackColor = ColorUtil.interpolateColor(offColor, onColor, progress);
        int radius = height / 2;
        roundedRectSimple(ctx, x, y, width, height, radius, trackColor);
        int knobSize = height - 4;
        int knobRadius = knobSize / 2;
        int knobX = (int) (x + 2 + (width - knobSize - 4) * progress);
        roundedRectSimple(ctx, knobX - 1, y + 1, knobSize + 2, knobSize + 2, knobRadius + 1,
                ColorUtil.withAlpha(0xFF000000, 25));
        roundedRectSimple(ctx, knobX, y + 2, knobSize, knobSize, knobRadius, 0xFFFFFFFF);
    }

    // ─── Progress Bar ─────────────────────────────────────────

    public static void progressBar(DrawContext ctx, int x, int y, int width, int height,
                                   float progress, int trackColor, int fillLeft, int fillRight, int radius) {
        roundedRectSimple(ctx, x, y, width, height, radius, trackColor);
        int fillWidth = (int) (width * Math.max(0, Math.min(1, progress)));
        if (fillWidth > 0) {
            roundedRectGradientH(ctx, x, y, fillWidth, height, Math.max(radius, 1), fillLeft, fillRight);
        }
    }

    // ─── Image Rendering ──────────────────────────────────────

    public static void drawTexturedQuad(DrawContext ctx, Identifier texture, int x, int y, int width, int height) {
        ctx.drawTexturedQuad(texture, x, x + width, y, y + height, 0f, 1f, 0f, 1f);
    }

    // ─── Scissoring ───────────────────────────────────────────

    public static void enableScissor(DrawContext ctx, int x, int y, int width, int height) {
        ctx.enableScissor(x, y, x + width, y + height);
    }

    public static void enableScissor(int x, int y, int width, int height) {
        double scale = mc.getWindow().getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale),
                (int) ((mc.getWindow().getScaledHeight() - y - height) * scale),
                (int) (width * scale), (int) (height * scale));
    }

    public static void disableScissor() { GL11.glDisable(GL11.GL_SCISSOR_TEST); }
    public static void disableScissor(DrawContext ctx) { ctx.disableScissor(); }

    // ─── Text Helpers ─────────────────────────────────────────

    public static void drawCenteredText(DrawContext ctx, String text, int centerX, int y, int color) {
        ctx.drawCenteredTextWithShadow(mc.textRenderer, text, centerX, y, color);
    }

    /**
     * Draw text with per-character theme gradient coloring.
     */
    public static void drawGradientText(DrawContext ctx, String text, int x, int y,
                                        float baseOffset, float offsetStep) {
        int drawX = x;
        for (int i = 0; i < text.length(); i++) {
            Color charColor = ThemeUtil.getThemeColor(baseOffset + i * offsetStep, ThemeType.ARRAYLIST, 1);
            ctx.drawTextWithShadow(mc.textRenderer, String.valueOf(text.charAt(i)), drawX, y,
                    ColorUtil.toARGB(charColor));
            drawX += mc.textRenderer.getWidth(String.valueOf(text.charAt(i)));
        }
    }

    public static void gradientLine(DrawContext ctx, int x, int y, int width, int height,
                                    int leftColor, int rightColor) {
        horizontalGradient(ctx, x, y, width, height, leftColor, rightColor);
    }
}
