package me.totalchaos01.chaosclient.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Optimized rendering utility for ChaosClient v1.1.1.
 * Includes smooth line drawing, blur simulation, skin face rendering,
 * and all gradient/rounded rect utilities.
 */
public final class RenderUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Matrix4f savedModelView;
    private static Matrix4f savedProjection;

    private RenderUtil() {}

    // ─── Matrix capture for world-to-screen ───────────────────

    public static void captureMatrices() {
        if (mc.gameRenderer == null || mc.options == null) return;
        var camera = mc.gameRenderer.getCamera();
        if (camera == null) return;
        savedModelView = new Matrix4f();
        savedModelView.identity();
        savedModelView.rotateX((float) Math.toRadians(camera.getPitch()));
        savedModelView.rotateY((float) Math.toRadians(camera.getYaw() + 180.0f));
        savedProjection = mc.gameRenderer.getBasicProjectionMatrix(mc.options.getFov().getValue());
    }

    public static double[] worldToScreen(double wx, double wy, double wz) {
        if (savedModelView == null || savedProjection == null || mc.gameRenderer == null) return null;
        var camera = mc.gameRenderer.getCamera();
        if (camera == null) return null;
        Vec3d camPos = camera.getCameraPos();
        float fx = (float) (wx - camPos.x);
        float fy = (float) (wy - camPos.y);
        float fz = (float) (wz - camPos.z);
        Vector4f pos = new Vector4f(fx, fy, fz, 1.0f);
        pos.mul(savedModelView);
        pos.mul(savedProjection);
        if (pos.w <= 0.0f) return null;
        float ndcX = pos.x / pos.w;
        float ndcY = pos.y / pos.w;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double screenX = (ndcX + 1.0) / 2.0 * sw;
        double screenY = (1.0 - ndcY) / 2.0 * sh;
        return new double[]{screenX, screenY};
    }

    // ─── Basic Rectangles ─────────────────────────────────────

    public static void rect(DrawContext ctx, double x, double y, double width, double height, int color) {
        ctx.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    public static void rectAbs(DrawContext ctx, double x1, double y1, double x2, double y2, int color) {
        ctx.fill((int) x1, (int) y1, (int) x2, (int) y2, color);
    }

    // ─── Smooth Line Drawing ──────────────────────────────────

    /**
     * Draw a smooth 2D line using per-pixel plotting (Bresenham-style).
     * Much smoother than segment-based fill, no visible stair-stepping.
     */
    public static void drawSmoothLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                       float lineWidth, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5) return;

        int lw = Math.max(1, Math.round(lineWidth));
        int hlw = lw / 2;
        int steps = Math.max(1, Math.min((int) (len * 0.4), 80));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int px = (int) Math.round(x1 + dx * t);
            int py = (int) Math.round(y1 + dy * t);
            ctx.fill(px - hlw - 1, py - hlw - 1, px + hlw + 2, py + hlw + 2, color);
        }
    }

    /**
     * Smooth gradient line: color fades from startColor to endColor.
     */
    public static void drawGradientLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                         float lineWidth, int startColor, int endColor) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5) return;

        int lw = Math.max(1, Math.round(lineWidth));
        int hlw = lw / 2;
        int steps = Math.max(1, Math.min((int) (len * 0.4), 80));

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int px = (int) Math.round(x1 + dx * t);
            int py = (int) Math.round(y1 + dy * t);
            int color = ColorUtil.interpolateColor(startColor, endColor, (float) t);
            ctx.fill(px - hlw - 1, py - hlw - 1, px + hlw + 2, py + hlw + 2, color);
        }
    }

    /**
     * Old-style line drawing (for backwards compatibility).
     */
    public static void drawLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                float lineWidth, int color) {
        drawSmoothLine(ctx, x1, y1, x2, y2, lineWidth, color);
    }

    // ─── Blur Simulation ──────────────────────────────────────

    /**
     * Simulated frosted-glass blur by layering semi-transparent fills.
     */
    public static void blurRect(DrawContext ctx, int x, int y, int w, int h, int layers) {
        for (int i = 0; i < layers; i++) {
            int alpha = Math.min(255, 30 + i * 15);
            ctx.fill(x - i, y - i, x + w + i, y + h + i, (alpha << 24) | 0x0D0D14);
        }
    }

    /**
     * Full-screen blur overlay.
     */
    public static void blurBackground(DrawContext ctx, int screenW, int screenH, int intensity) {
        for (int i = 0; i < intensity; i++) {
            int alpha = 20 + i * 8;
            ctx.fill(0, 0, screenW, screenH, (Math.min(alpha, 200) << 24) | 0x08080E);
        }
    }

    // ─── Player Skin Face ─────────────────────────────────────

    /**
     * Draw a player's face (8x8 from skin texture) at the given position.
     */
    public static void drawPlayerFace(DrawContext ctx, PlayerEntity player, int x, int y, int size) {
        if (player == null || mc.getNetworkHandler() == null) return;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) return;

        var skinTextures = entry.getSkinTextures();
        // Use MC's built-in PlayerSkinDrawer for proper face rendering
        PlayerSkinDrawer.draw(ctx, skinTextures, x, y, size);
    }

    // ─── Optimized Rounded Rectangles ─────────────────────────

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

    public static void roundedRectGradientH(DrawContext ctx, int x, int y, int width, int height,
                                            int radius, int leftColor, int rightColor) {
        if (width <= 0 || height <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);
        if (leftColor == rightColor || width <= 2) {
            roundedRectSimple(ctx, x, y, width, height, radius, leftColor);
            return;
        }
        for (int col = 0; col < radius; col++) {
            float tl = (float) col / Math.max(1, width - 1);
            float tr = (float) (width - 1 - col) / Math.max(1, width - 1);
            int colorL = ColorUtil.interpolateColor(leftColor, rightColor, tl);
            int colorR = ColorUtil.interpolateColor(leftColor, rightColor, tr);
            int dxc = radius - col;
            int yOff = radius - (int) Math.sqrt((double) radius * radius - (double) dxc * dxc);
            ctx.fill(x + col, y + yOff, x + col + 1, y + height - yOff, colorL);
            ctx.fill(x + width - 1 - col, y + yOff, x + width - col, y + height - yOff, colorR);
        }
        int bodyLeft = x + radius, bodyRight = x + width - radius, bodyWidth = bodyRight - bodyLeft;
        if (bodyWidth > 0) {
            int numStrips = Math.min(bodyWidth, 20);
            float stripW = (float) bodyWidth / numStrips;
            for (int s = 0; s < numStrips; s++) {
                float midCol = s * stripW + stripW / 2f;
                float t = (radius + midCol) / Math.max(1, width - 1);
                int color = ColorUtil.interpolateColor(leftColor, rightColor, t);
                int sx = bodyLeft + Math.round(s * stripW);
                int ex = (s == numStrips - 1) ? bodyRight : bodyLeft + Math.round((s + 1) * stripW);
                if (ex > sx) ctx.fill(sx, y, ex, y + height, color);
            }
        }
    }

    public static void roundedRectGradientV(DrawContext ctx, int x, int y, int width, int height,
                                            int radius, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;
        radius = Math.min(radius, Math.min(width, height) / 2);
        if (topColor == bottomColor || height <= 2) {
            roundedRectSimple(ctx, x, y, width, height, radius, topColor);
            return;
        }
        for (int row = 0; row < radius; row++) {
            float tt = (float) row / Math.max(1, height - 1);
            float tb = (float) (height - 1 - row) / Math.max(1, height - 1);
            int colorT = ColorUtil.interpolateColor(topColor, bottomColor, tt);
            int colorB = ColorUtil.interpolateColor(topColor, bottomColor, tb);
            int dy = radius - row;
            int xOff = radius - (int) Math.sqrt((double) radius * radius - (double) dy * dy);
            ctx.fill(x + xOff, y + row, x + width - xOff, y + row + 1, colorT);
            ctx.fill(x + xOff, y + height - 1 - row, x + width - xOff, y + height - row, colorB);
        }
        int bodyTop = y + radius, bodyBottom = y + height - radius, bodyHeight = bodyBottom - bodyTop;
        if (bodyHeight > 0) {
            int numStrips = Math.min(bodyHeight, 20);
            float stripH = (float) bodyHeight / numStrips;
            for (int s = 0; s < numStrips; s++) {
                float midRow = s * stripH + stripH / 2f;
                float t = (radius + midRow) / Math.max(1, height - 1);
                int color = ColorUtil.interpolateColor(topColor, bottomColor, t);
                int sy = bodyTop + Math.round(s * stripH);
                int ey = (s == numStrips - 1) ? bodyBottom : bodyTop + Math.round((s + 1) * stripH);
                if (ey > sy) ctx.fill(x, sy, x + width, ey, color);
            }
        }
    }

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
        int strips = Math.min(Math.max(1, (int) width), 20);
        float stripW = (float) width / strips;
        for (int i = 0; i < strips; i++) {
            float t = (float) i / Math.max(1, strips - 1);
            int color = ColorUtil.interpolateColor(leftColor, rightColor, t);
            int sx = (int) (x + i * stripW);
            int ex = (i == strips - 1) ? (int) (x + width) : (int) (x + (i + 1) * stripW);
            ctx.fill(sx, (int) y, ex, (int) (y + height), color);
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

    // ─── Shadows & Glow ───────────────────────────────────────

    public static void shadow(DrawContext ctx, double x, double y, double width, double height,
                              double radius, int shadowColor) {
        shadow(ctx, x, y, width, height, radius, shadowColor, 10, 2.0);
    }

    public static void shadow(DrawContext ctx, double x, double y, double width, double height,
                              double radius, int shadowColor, int layers, double spread) {
        int baseAlpha = (shadowColor >> 24) & 0xFF;
        for (int i = layers; i > 0; i--) {
            int expand = (int) (i * spread);
            double progress = (double) i / layers;
            int alpha = (int) (baseAlpha * Math.pow(1.0 - progress, 3) * 0.4);
            if (alpha <= 0) continue;
            int color = (alpha << 24) | (shadowColor & 0x00FFFFFF);
            roundedRectSimple(ctx, (int) (x - expand), (int) (y - expand),
                    (int) (width + expand * 2), (int) (height + expand * 2),
                    (int) (radius + expand * 0.8), color);
        }
    }

    public static void glow(DrawContext ctx, double x, double y, double width, double height,
                            double radius, Color glowColor, int intensity) {
        int layers = Math.min(intensity, 3);
        for (int i = layers; i > 0; i--) {
            int expand = (int) (i * 2.0);
            double progress = (double) i / layers;
            int alpha = (int) (glowColor.getAlpha() * (1.0 - progress * progress) * 0.25);
            if (alpha <= 0) continue;
            int color = (alpha << 24) | (glowColor.getRGB() & 0x00FFFFFF);
            roundedRectSimple(ctx, (int) (x - expand), (int) (y - expand),
                    (int) (width + expand * 2), (int) (height + expand * 2),
                    (int) (radius + expand), color);
        }
    }

    // ─── Toggle Switch ────────────────────────────────────────

    public static void toggleSwitch(DrawContext ctx, int x, int y, int width, int height,
                                    float progress, int offColor, int onColor) {
        int trackColor = ColorUtil.interpolateColor(offColor, onColor, progress);
        int radius = height / 2;
        roundedRectSimple(ctx, x, y, width, height, radius, trackColor);
        int knobSize = height - 4;
        int knobRadius = knobSize / 2;
        int knobX = (int) (x + 2 + (width - knobSize - 4) * progress);
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

    // ─── Image / Texture ──────────────────────────────────────

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
