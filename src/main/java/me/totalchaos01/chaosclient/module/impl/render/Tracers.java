package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Completely redesigned Tracers with smooth gradient lines,
 * theme-based colors, interpolated positions, and distance-based fading.
 * No more jitter - uses smoothed endpoint tracking.
 */
@ModuleInfo(name = "Tracers", description = "Draws lines to entities", category = Category.RENDER)
public class Tracers extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting hostileMobs = new BooleanSetting("Hostile Mobs", true);
    private final BooleanSetting passiveMobs = new BooleanSetting("Passive Mobs", false);
    private final BooleanSetting distanceColor = new BooleanSetting("Distance Color", true);
    private final BooleanSetting themeGradient = new BooleanSetting("Theme Gradient", true);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3, 0.5);

    // Smoothed screen positions to prevent jitter
    private final Map<UUID, double[]> smoothedPositions = new HashMap<>();
    private static final double SMOOTH_FACTOR = 0.35;

    public Tracers() {
        addSettings(players, hostileMobs, passiveMobs, distanceColor, themeGradient, range, lineWidth);
    }

    @Override
    protected void onDisable() {
        smoothedPositions.clear();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        double centerX = screenW / 2.0;
        double centerY = screenH / 2.0;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValidTarget(entity)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist > range.getValue()) continue;

            renderTracer(ctx, entity, tickDelta, centerX, centerY, dist);
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity instanceof PlayerEntity && players.isEnabled()) return true;
        if (entity instanceof HostileEntity && hostileMobs.isEnabled()) return true;
        if (entity instanceof PassiveEntity && passiveMobs.isEnabled()) return true;
        return false;
    }

    private void renderTracer(DrawContext ctx, Entity entity, float tickDelta,
                              double centerX, double centerY, double dist) {
        Vec3d pos = entity.getLerpedPos(tickDelta);
        double entityCenterY = pos.y + entity.getHeight() / 2.0;

        double[] screenPos = RenderUtil.worldToScreen(pos.x, entityCenterY, pos.z);
        if (screenPos == null) return;

        // Smooth the screen-space position to reduce jitter
        UUID uid = entity.getUuid();
        double[] smoothed = smoothedPositions.get(uid);
        if (smoothed == null) {
            smoothed = new double[]{screenPos[0], screenPos[1]};
            smoothedPositions.put(uid, smoothed);
        } else {
            smoothed[0] += (screenPos[0] - smoothed[0]) * SMOOTH_FACTOR;
            smoothed[1] += (screenPos[1] - smoothed[1]) * SMOOTH_FACTOR;
        }

        double targetX = smoothed[0];
        double targetY = smoothed[1];

        // Calculate colors
        float maxRange = (float) range.getValue();
        float distRatio = Math.min(1f, (float) dist / maxRange);
        float lw = (float) lineWidth.getValue();

        // Distance-based line width (closer = slightly thicker)
        float dynamicWidth = lw + (1f - distRatio) * 0.5f;

        if (themeGradient.isEnabled()) {
            // Theme gradient tracer
            float offset = (float) (entity.getId() * 0.3);
            Color startColor = ThemeUtil.getThemeColor(offset, ThemeType.ARRAYLIST, 1);
            Color endColor = ThemeUtil.getThemeColor(offset + 0.5f, ThemeType.ARRAYLIST, 1);

            // Distance-based alpha
            int startAlpha = distanceColor.isEnabled() ? (int) (255 * (1f - distRatio * 0.6f)) : 200;
            int endAlpha = distanceColor.isEnabled() ? (int) (255 * (1f - distRatio * 0.3f)) : 200;

            int startARGB = ColorUtil.withAlpha(ColorUtil.toARGB(startColor), startAlpha);
            int endARGB = ColorUtil.withAlpha(ColorUtil.toARGB(endColor), endAlpha);

            RenderUtil.drawGradientLine(ctx, centerX, centerY, targetX, targetY,
                    dynamicWidth, startARGB, endARGB);
        } else if (distanceColor.isEnabled()) {
            // Distance-based color: green (close) -> yellow -> red (far)
            int alpha = (int) (255 * (1f - distRatio * 0.5f));
            int color = getDistanceColor(distRatio, alpha);
            RenderUtil.drawSmoothLine(ctx, centerX, centerY, targetX, targetY,
                    dynamicWidth, color);
        } else {
            // Simple white tracer
            int alpha = (int) (200 * (1f - distRatio * 0.4f));
            int color = ColorUtil.withAlpha(0xFFFFFFFF, alpha);
            RenderUtil.drawSmoothLine(ctx, centerX, centerY, targetX, targetY,
                    dynamicWidth, color);
        }
    }

    private int getDistanceColor(float ratio, int alpha) {
        int r, g;
        if (ratio < 0.5f) {
            float t = ratio * 2f;
            r = (int) (255 * t);
            g = 255;
        } else {
            float t = (ratio - 0.5f) * 2f;
            r = 255;
            g = (int) (255 * (1f - t));
        }
        return (alpha << 24) | (r << 16) | (g << 8);
    }
}
