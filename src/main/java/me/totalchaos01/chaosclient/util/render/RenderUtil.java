package me.totalchaos01.chaosclient.util.render;

import me.totalchaos01.chaosclient.font.ChaosFont;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
    private static Vec3d savedCameraPos;

    private RenderUtil() {}

    // ─── Matrix capture for world-to-screen ───────────────────

    public static void captureMatrices() {
        captureMatrices(1.0f);
    }

    public static void captureMatrices(float tickDelta) {
        try {
            if (mc.gameRenderer == null || mc.options == null) return;
            Camera camera = mc.gameRenderer.getCamera();
            if (camera == null) return;

            // Build view matrix from camera rotation
            Quaternionf invRot = new Quaternionf(camera.getRotation()).conjugate();
            savedModelView = new Matrix4f().rotation(invRot);

            // Build projection matrix from current FOV
            // In 1.21.11 RenderSystem no longer exposes a Matrix4f projection getter,
            // so we reconstruct from the game renderer's FOV setting
            float fov = mc.options.getFov().getValue().floatValue();
            savedProjection = mc.gameRenderer.getBasicProjectionMatrix(fov);

            savedCameraPos = camera.getCameraPos();
        } catch (Exception ignored) {
            savedModelView = null;
            savedProjection = null;
            savedCameraPos = null;
        }
    }

    public static double[] worldToScreen(double wx, double wy, double wz) {
        if (savedModelView == null || savedProjection == null || savedCameraPos == null) return null;

        Vec3d camPos = savedCameraPos;
        float relX = (float) (wx - camPos.x);
        float relY = (float) (wy - camPos.y);
        float relZ = (float) (wz - camPos.z);
        Vector4f pos = new Vector4f(relX, relY, relZ, 1.0f);
        pos.mul(savedModelView);
        pos.mul(savedProjection);
        if (pos.w <= 0.0f) return null;

        float ndcX = pos.x / pos.w;
        float ndcY = pos.y / pos.w;
        float ndcZ = pos.z / pos.w;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double screenX = (ndcX + 1.0) / 2.0 * sw;
        double screenY = (1.0 - ndcY) / 2.0 * sh;
        double depth = (ndcZ + 1.0) / 2.0;
        return new double[]{screenX, screenY, depth};
    }

    // ─── Basic Rectangles ─────────────────────────────────────

    public static void rect(DrawContext ctx, double x, double y, double width, double height, int color) {
        ctx.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
    }

    public static void rectAbs(DrawContext ctx, double x1, double y1, double x2, double y2, int color) {
        ctx.fill((int) x1, (int) y1, (int) x2, (int) y2, color);
    }

    // ─── Smooth Line Drawing (GPU-accelerated via matrix rotation) ───

    /**
     * Draw a smooth 2D line using a matrix-rotated rectangle.
     * The GPU handles rotation — produces perfectly smooth lines in 1 draw call.
     */
    public static void drawSmoothLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                       float lineWidth, int color) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5) return;

        float angle = (float) Math.atan2(dy, dx);
        int iLen = Math.max(1, (int) Math.ceil(len));
        int halfW = Math.max(0, (Math.round(lineWidth) - 1) / 2);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) x1, (float) y1);
        ctx.getMatrices().rotate(angle);
        ctx.fill(0, -halfW, iLen, halfW + 1, color);
        ctx.getMatrices().popMatrix();
    }

    /**
     * Smooth gradient line using matrix rotation + color segments.
     */
    public static void drawGradientLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                         float lineWidth, int startColor, int endColor) {
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5) return;

        float angle = (float) Math.atan2(dy, dx);
        int iLen = Math.max(1, (int) Math.ceil(len));
        int halfW = Math.max(0, (Math.round(lineWidth) - 1) / 2);
        int segments = Math.max(1, Math.min(iLen / 3, 40));

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) x1, (float) y1);
        ctx.getMatrices().rotate(angle);
        for (int i = 0; i < segments; i++) {
            int sx = iLen * i / segments;
            int ex = iLen * (i + 1) / segments;
            float t = (float) i / Math.max(1, segments - 1);
            int color = ColorUtil.interpolateColor(startColor, endColor, t);
            ctx.fill(sx, -halfW, ex, halfW + 1, color);
        }
        ctx.getMatrices().popMatrix();
    }

    /**
     * Glow line: wider semi-transparent line behind, then crisp main line.
     */
    public static void drawGlowLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                    float lineWidth, int color) {
        int alpha = (color >> 24) & 0xFF;
        int glowColor = ColorUtil.withAlpha(color, Math.min(50, alpha));
        drawSmoothLine(ctx, x1, y1, x2, y2, lineWidth + 4.0f, glowColor);
        drawSmoothLine(ctx, x1, y1, x2, y2, lineWidth, color);
    }

    /**
     * Old-style line drawing (for backwards compatibility).
     */
    public static void drawLine(DrawContext ctx, double x1, double y1, double x2, double y2,
                                float lineWidth, int color) {
        drawSmoothLine(ctx, x1, y1, x2, y2, lineWidth, color);
    }

    // ─── Blur (real GPU Gaussian blur) ──────────────────────────

    /**
     * Apply real GPU blur to a rect area, with a dark tint overlay.
     * Safely handles "Can only blur once per frame" limitation.
     */
    public static void blurRect(DrawContext ctx, int x, int y, int w, int h, int layers) {
        try {
            ctx.applyBlur();
        } catch (IllegalStateException ignored) {
            // Blur already applied this frame — skip
        }
        ctx.fill(x, y, x + w, y + h, 0x80080810);
    }

    /**
     * Full-screen GPU blur + dark overlay for frosted glass effect.
     * Safely handles "Can only blur once per frame" limitation.
     */
    public static void blurBackground(DrawContext ctx, int screenW, int screenH, int intensity) {
        try {
            ctx.applyBlur();
        } catch (IllegalStateException ignored) {
            // Blur already applied this frame — skip
        }
        int alpha = Math.min(0x60, intensity * 12);
        ctx.fill(0, 0, screenW, screenH, (alpha << 24) | 0x08080E);
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
        int baseAlpha = (color >> 24) & 0xFF;
        if (baseAlpha == 0) return;
        if (radius <= 0) { ctx.fill(x, y, x + width, y + height, color); return; }
        radius = Math.min(radius, Math.min(width, height) / 2);
        int rgb = color & 0x00FFFFFF;

        // Body fill: center + left side + right side
        ctx.fill(x + radius, y, x + width - radius, y + height, color);
        ctx.fill(x, y + radius, x + radius, y + height - radius, color);
        ctx.fill(x + width - radius, y + radius, x + width, y + height - radius, color);

        // Corner scanlines with anti-aliased edge pixels
        for (int i = 0; i <= radius; i++) {
            double exact = radius - Math.sqrt((double) radius * radius - (double) (radius - i) * (radius - i));
            int offset = (int) exact;
            double frac = exact - offset;

            // Full corner pixels
            ctx.fill(x + offset, y + i, x + radius, y + i + 1, color);
            ctx.fill(x + width - radius, y + i, x + width - offset, y + i + 1, color);
            ctx.fill(x + offset, y + height - i - 1, x + radius, y + height - i, color);
            ctx.fill(x + width - radius, y + height - i - 1, x + width - offset, y + height - i, color);

            // Anti-aliased edge pixel (sub-pixel smooth transition)
            if (offset > 0 && frac > 0.05) {
                int aaAlpha = (int) ((1.0 - frac) * baseAlpha);
                int aaColor = (aaAlpha << 24) | rgb;
                ctx.fill(x + offset - 1, y + i, x + offset, y + i + 1, aaColor);
                ctx.fill(x + width - offset, y + i, x + width - offset + 1, y + i + 1, aaColor);
                ctx.fill(x + offset - 1, y + height - i - 1, x + offset, y + height - i, aaColor);
                ctx.fill(x + width - offset, y + height - i - 1, x + width - offset + 1, y + height - i, aaColor);
            }
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
        int scaledIntensity = Math.max(1, (int) Math.round(intensity * ThemeUtil.getGlowIntensity()));
        int layers = Math.min(scaledIntensity, 6);
        for (int i = layers; i > 0; i--) {
            int expand = (int) (i * 2.0);
            double progress = (double) i / layers;
            int alpha = (int) (glowColor.getAlpha() * (1.0 - progress * progress) * 0.25 * ThemeUtil.getGlowIntensity());
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
        ctx.drawCenteredTextWithShadow(ChaosFont.renderer(), text, centerX, y, color);
    }

    public static void drawGradientText(DrawContext ctx, String text, int x, int y,
                                        float baseOffset, float offsetStep) {
        int drawX = x;
        for (int i = 0; i < text.length(); i++) {
            Color charColor = ThemeUtil.getThemeColor(baseOffset + i * offsetStep, ThemeType.ARRAYLIST, 1);
            ChaosFont.drawWithShadow(ctx, String.valueOf(text.charAt(i)), drawX, y,
                    ColorUtil.toARGB(charColor));
            drawX += ChaosFont.getWidth(String.valueOf(text.charAt(i)));
        }
    }

    public static void gradientLine(DrawContext ctx, int x, int y, int width, int height,
                                    int leftColor, int rightColor) {
        horizontalGradient(ctx, x, y, width, height, leftColor, rightColor);
    }

    // ─── Color Picker Components ──────────────────────────────────

    /**
     * Draw a horizontal hue bar (rainbow gradient) with a selection indicator.
     */
    public static void drawHueBar(DrawContext ctx, int x, int y, int width, int height, float selectedHue) {
        int strips = Math.min(width, 60);
        float stripW = (float) width / strips;
        for (int i = 0; i < strips; i++) {
            float hue = (float) i / strips;
            int color = Color.HSBtoRGB(hue, 1f, 1f) | 0xFF000000;
            int sx = x + Math.round(i * stripW);
            int ex = (i == strips - 1) ? x + width : x + Math.round((i + 1) * stripW);
            ctx.fill(sx, y, ex, y + height, color);
        }
        // Selection indicator
        int ix = x + Math.round(selectedHue * (width - 2));
        ctx.fill(ix - 1, y - 2, ix + 2, y + height + 2, 0xFFFFFFFF);
        ctx.fill(ix, y - 1, ix + 1, y + height + 1, 0xFF000000);
    }

    /**
     * Draw a saturation-brightness picker grid for a given hue value.
     * X-axis = saturation (0–1), Y-axis = brightness (1–0 top-to-bottom).
     */
    public static void drawSBPicker(DrawContext ctx, int x, int y, int width, int height,
                                     float hue, float selectedSat, float selectedBri) {
        int cols = Math.min(width / 3, 40);
        int rows = Math.min(height / 3, 25);
        float cellW = (float) width / cols;
        float cellH = (float) height / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float sat = (float) c / Math.max(1, cols - 1);
                float bri = 1.0f - (float) r / Math.max(1, rows - 1);
                int color = Color.HSBtoRGB(hue, sat, bri) | 0xFF000000;
                int cx1 = x + Math.round(c * cellW);
                int cy1 = y + Math.round(r * cellH);
                int cx2 = (c == cols - 1) ? x + width : x + Math.round((c + 1) * cellW);
                int cy2 = (r == rows - 1) ? y + height : y + Math.round((r + 1) * cellH);
                ctx.fill(cx1, cy1, cx2, cy2, color);
            }
        }

        // Selection crosshair
        int selX = x + Math.round(selectedSat * (width - 1));
        int selY = y + Math.round((1.0f - selectedBri) * (height - 1));
        circleOutline(ctx, selX, selY, 4, 1, 0xFFFFFFFF);
        circleOutline(ctx, selX, selY, 3, 1, 0xFF000000);
    }
}
