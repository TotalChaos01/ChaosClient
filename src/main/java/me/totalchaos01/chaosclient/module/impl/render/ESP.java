package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

/**
 * Completely redesigned ESP with themed colors, smooth rendering,
 * corner-style and box modes, health bars and distance indicators.
 * Inspired by Rise client visuals adapted for modern MC.
 */
@ModuleInfo(name = "ESP", description = "Highlights entities through walls", category = Category.RENDER)
public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Corner", "Box", "Corner", "Glow");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting hostileMobs = new BooleanSetting("Hostile Mobs", true);
    private final BooleanSetting passiveMobs = new BooleanSetting("Passive Mobs", false);
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting distanceText = new BooleanSetting("Distance", true);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 4, 0.5);

    public ESP() {
        addSettings(mode, players, hostileMobs, passiveMobs, healthBar, distanceText, range, lineWidth);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!isValidTarget(entity)) continue;
            if (mc.player.distanceTo(entity) > range.getValue()) continue;

            renderEntityESP(ctx, entity, tickDelta);
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity instanceof PlayerEntity && players.isEnabled()) return true;
        if (entity instanceof HostileEntity && hostileMobs.isEnabled()) return true;
        if (entity instanceof PassiveEntity && passiveMobs.isEnabled()) return true;
        return false;
    }

    private void renderEntityESP(DrawContext ctx, Entity entity, float tickDelta) {
        Vec3d pos = entity.getLerpedPos(tickDelta);
        Box bb = entity.getBoundingBox();
        double halfW = (bb.maxX - bb.minX) / 2.0;
        double height = bb.maxY - bb.minY;

        // Project 8 corners to screen space
        double[][] corners = {
                {pos.x - halfW, pos.y, pos.z - halfW},
                {pos.x + halfW, pos.y, pos.z - halfW},
                {pos.x - halfW, pos.y, pos.z + halfW},
                {pos.x + halfW, pos.y, pos.z + halfW},
                {pos.x - halfW, pos.y + height, pos.z - halfW},
                {pos.x + halfW, pos.y + height, pos.z - halfW},
                {pos.x - halfW, pos.y + height, pos.z + halfW},
                {pos.x + halfW, pos.y + height, pos.z + halfW},
        };

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean anyVisible = false;
        int visibleCount = 0;

        for (double[] corner : corners) {
            double[] screen = RenderUtil.worldToScreen(corner[0], corner[1], corner[2]);
            if (screen == null || screen[2] < 0 || screen[2] > 1.0) continue;
            visibleCount++;
            anyVisible = true;
            minX = Math.min(minX, screen[0]);
            minY = Math.min(minY, screen[1]);
            maxX = Math.max(maxX, screen[0]);
            maxY = Math.max(maxY, screen[1]);
        }

        if (visibleCount < 4) return;

        int ix = (int) minX, iy = (int) minY;
        int bw = (int) (maxX - minX), bh = (int) (maxY - minY);
        if (bw < 2 || bh < 2) return;

        // Get theme color
        float offset = (float) (entity.getId() * 0.3);
        Color themeColor = ThemeUtil.getThemeColor(offset, ThemeType.ARRAYLIST, 1);
        int themeARGB = ColorUtil.toARGB(themeColor);
        int themeWithAlpha = ColorUtil.withAlpha(themeARGB, 180);
        int bgColor = 0x40000000;

        float lw = (float) lineWidth.getValue();
        int t = Math.max(1, Math.round(lw));

        switch (mode.getMode()) {
            case "Box" -> {
                // Black outline behind
                drawOutline(ctx, ix - 1, iy - 1, bw + 2, bh + 2, 1, 0xAA000000);
                // Semi-transparent fill
                ctx.fill(ix, iy, ix + bw, iy + bh, bgColor);
                // Themed outline
                drawOutline(ctx, ix, iy, bw, bh, t, themeWithAlpha);
                // Subtle inner shadow line
                if (bw > 4 && bh > 4) {
                    drawOutline(ctx, ix + 1, iy + 1, bw - 2, bh - 2, 1, 0x30000000);
                }
            }
            case "Corner" -> {
                int cornerLen = Math.max(4, Math.min(bw, bh) / 4);
                // Black outline behind corners for contrast
                drawCorners(ctx, ix - 1, iy - 1, bw + 2, bh + 2, cornerLen + 1, 1, 0xAA000000);
                // Themed corners
                drawCorners(ctx, ix, iy, bw, bh, cornerLen, t, themeWithAlpha);
            }
            case "Glow" -> {
                // Glow effect
                for (int i = 8; i > 0; i--) {
                    int alpha = (int) (45 * (1.0 - (double) i / 8));
                    int glowCol = ColorUtil.withAlpha(themeARGB, alpha);
                    ctx.fill(ix - i, iy - i, ix + bw + i, iy + bh + i, glowCol);
                }
                // Inner fill
                ctx.fill(ix, iy, ix + bw, iy + bh, 0x20000000);
                // Core outline
                drawOutline(ctx, ix, iy, bw, bh, 1, themeWithAlpha);
            }
        }

        // Health bar on the left side
        if (healthBar.isEnabled() && entity instanceof LivingEntity living) {
            float hp = living.getHealth();
            float maxHp = living.getMaxHealth();
            float hpRatio = Math.max(0, Math.min(1, hp / maxHp));

            int barWidth = 2;
            int barX = ix - barWidth - 3;
            int barHeight = bh;
            int filledHeight = (int) (barHeight * hpRatio);

            // Background
            ctx.fill(barX - 1, iy - 1, barX + barWidth + 1, iy + barHeight + 1, 0xAA000000);
            // Empty bar
            ctx.fill(barX, iy, barX + barWidth, iy + barHeight, 0xFF1A1A1A);
            // Filled bar (green to red gradient)
            int healthColor = getHealthColor(hpRatio);
            if (filledHeight > 0) {
                ctx.fill(barX, iy + barHeight - filledHeight, barX + barWidth, iy + barHeight, healthColor);
            }
        }

        // Distance text below box
        if (distanceText.isEnabled()) {
            double dist = mc.player.distanceTo(entity);
            String distStr = String.format("%.1fm", dist);
            int textWidth = mc.textRenderer.getWidth(distStr);
            int textX = ix + (bw - textWidth) / 2;
            int textY = iy + bh + 3;

            // Background for text
            RenderUtil.roundedRectSimple(ctx, textX - 3, textY - 1, textWidth + 6, 11, 3, 0xCC000000);
            ctx.drawTextWithShadow(mc.textRenderer, distStr, textX, textY, 0xFFCCCCCC);
        }
    }

    private void drawOutline(DrawContext ctx, int x, int y, int w, int h, int t, int color) {
        ctx.fill(x, y, x + w, y + t, color);             // top
        ctx.fill(x, y + h - t, x + w, y + h, color);     // bottom
        ctx.fill(x, y, x + t, y + h, color);             // left
        ctx.fill(x + w - t, y, x + w, y + h, color);     // right
    }

    private void drawCorners(DrawContext ctx, int x, int y, int w, int h, int len, int t, int color) {
        // Top-left
        ctx.fill(x, y, x + len, y + t, color);
        ctx.fill(x, y, x + t, y + len, color);
        // Top-right
        ctx.fill(x + w - len, y, x + w, y + t, color);
        ctx.fill(x + w - t, y, x + w, y + len, color);
        // Bottom-left
        ctx.fill(x, y + h - t, x + len, y + h, color);
        ctx.fill(x, y + h - len, x + t, y + h, color);
        // Bottom-right
        ctx.fill(x + w - len, y + h - t, x + w, y + h, color);
        ctx.fill(x + w - t, y + h - len, x + w, y + h, color);
    }

    private int getHealthColor(float ratio) {
        int r = (int) (255 * (1.0f - ratio));
        int g = (int) (255 * ratio);
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
