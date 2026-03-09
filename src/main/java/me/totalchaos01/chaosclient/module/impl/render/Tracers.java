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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

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
    private final BooleanSetting glow = new BooleanSetting("Glow", true);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.0, 0.5, 2.5, 0.25);

    public Tracers() {
        addSettings(players, hostileMobs, passiveMobs, distanceColor, themeGradient, glow, range, lineWidth);
    }

    @Override
    protected void onDisable() {
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen instanceof me.totalchaos01.chaosclient.ui.clickgui.ClickGuiScreen) return;
        if (mc.currentScreen != null && mc.currentScreen.shouldPause()) return;
        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        double centerX = screenW / 2.0;
        double centerY = screenH / 2.0;
        double rangeSq = range.getValue() * range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValidTarget(entity)) continue;
            if (!entity.isAlive()) continue;

            double distSq = mc.player.squaredDistanceTo(entity);
            if (distSq > rangeSq) continue;

            renderTracer(ctx, entity, tickDelta, centerX, centerY, Math.sqrt(distSq));
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity instanceof PlayerEntity && players.isEnabled()) return true;
        if (entity instanceof Monster && hostileMobs.isEnabled()) return true;
        if (entity instanceof MobEntity && !(entity instanceof Monster) && passiveMobs.isEnabled()) return true;
        return false;
    }

    private void renderTracer(DrawContext ctx, Entity entity, float tickDelta,
                              double centerX, double centerY, double dist) {
        // Use interpolated position for smooth tracking locked to entity
        Vec3d pos = entity.getLerpedPos(tickDelta);
        double entityCenterY = pos.y + entity.getHeight() / 2.0;

        double[] screenPos = RenderUtil.worldToScreen(pos.x, entityCenterY, pos.z);
        if (screenPos == null || screenPos.length < 3) return;
        if (screenPos[2] < 0.0 || screenPos[2] > 1.0) return;

        // Direct projection — no smoothing for tight entity lock
        double targetX = screenPos[0];
        double targetY = screenPos[1];

        // Calculate colors
        float maxRange = (float) range.getValue();
        float distRatio = Math.min(1f, (float) dist / maxRange);
        float lw = (float) lineWidth.getValue();

        // Distance-based line width (closer = slightly thicker)
        float dynamicWidth = Math.max(0.75f, lw + (1f - distRatio) * 0.2f);

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

                if (glow.isEnabled()) {
                RenderUtil.drawGlowLine(ctx, centerX, centerY, targetX, targetY,
                    dynamicWidth, endARGB);
                }
                RenderUtil.drawGradientLine(ctx, centerX, centerY, targetX, targetY,
                    dynamicWidth, startARGB, endARGB);
        } else if (distanceColor.isEnabled()) {
            // Distance-based color: green (close) -> yellow -> red (far)
            int alpha = (int) (255 * (1f - distRatio * 0.5f));
            int color = getDistanceColor(distRatio, alpha);
                if (glow.isEnabled()) {
                RenderUtil.drawGlowLine(ctx, centerX, centerY, targetX, targetY,
                    dynamicWidth, color);
                } else {
                RenderUtil.drawSmoothLine(ctx, centerX, centerY, targetX, targetY,
                    dynamicWidth, color);
                }
        } else {
            // Simple white tracer
            int alpha = (int) (200 * (1f - distRatio * 0.4f));
            int color = ColorUtil.withAlpha(0xFFFFFFFF, alpha);
            if (glow.isEnabled()) {
                RenderUtil.drawGlowLine(ctx, centerX, centerY, targetX, targetY,
                        dynamicWidth, color);
            } else {
                RenderUtil.drawSmoothLine(ctx, centerX, centerY, targetX, targetY,
                        dynamicWidth, color);
            }
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
