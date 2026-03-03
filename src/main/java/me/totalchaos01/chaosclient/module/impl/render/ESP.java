package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.event.events.EventRender3D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Renders ESP (Extra Sensory Perception) boxes/outlines around entities.
 * Projects 3D bounding boxes to 2D screen space for clean overlay rendering.
 */
@ModuleInfo(name = "ESP", description = "Highlights entities through walls", category = Category.RENDER)
public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Box", "Box", "Outline", "Glow");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting hostileMobs = new BooleanSetting("Hostile Mobs", true);
    private final BooleanSetting passiveMobs = new BooleanSetting("Passive Mobs", false);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 2, 1, 5, 0.5);

    // Colors: ARGB
    private static final int COLOR_PLAYER = 0xFFFF4444;
    private static final int COLOR_HOSTILE = 0xFFFF8844;
    private static final int COLOR_PASSIVE = 0xFF44FF44;

    public ESP() {
        addSettings(mode, players, hostileMobs, passiveMobs, range, lineWidth);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        // Glow mode: set entity glowing flag
        if (mode.is("Glow")) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                if (mc.player.distanceTo(entity) > range.getValue()) continue;

                if (shouldRender(entity)) {
                    entity.setGlowing(true);
                } else {
                    entity.setGlowing(false);
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.world == null || mc.player == null) return;
        if (mode.is("Glow")) return; // Glow mode handled in 3D event

        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (mc.player.distanceTo(entity) > range.getValue()) continue;
            if (!shouldRender(entity)) continue;

            // Get interpolated entity position
            Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
            double ix = lerpedPos.x;
            double iy = lerpedPos.y;
            double iz = lerpedPos.z;

            // Entity bounding box dimensions
            Box box = entity.getBoundingBox();
            double halfW = (box.maxX - box.minX) / 2.0;
            double halfD = (box.maxZ - box.minZ) / 2.0;
            double height = box.maxY - box.minY;

            // Project 8 corners of the bounding box to screen
            double[][] corners = {
                {ix - halfW, iy, iz - halfD},
                {ix + halfW, iy, iz - halfD},
                {ix - halfW, iy, iz + halfD},
                {ix + halfW, iy, iz + halfD},
                {ix - halfW, iy + height, iz - halfD},
                {ix + halfW, iy + height, iz - halfD},
                {ix - halfW, iy + height, iz + halfD},
                {ix + halfW, iy + height, iz + halfD}
            };

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean allVisible = true;

            for (double[] corner : corners) {
                double[] screen = RenderUtil.worldToScreen(corner[0], corner[1], corner[2]);
                if (screen == null) {
                    allVisible = false;
                    break;
                }
                minX = Math.min(minX, screen[0]);
                minY = Math.min(minY, screen[1]);
                maxX = Math.max(maxX, screen[0]);
                maxY = Math.max(maxY, screen[1]);
            }

            if (!allVisible) continue;

            int color = getEntityColor(entity);
            double w = maxX - minX;
            double h = maxY - minY;

            if (w < 1 || h < 1) continue;

            if (mode.is("Box")) {
                // Filled box with transparency
                int fillColor = (0x30 << 24) | (color & 0x00FFFFFF);
                RenderUtil.rect(ctx, minX, minY, w, h, fillColor);
                RenderUtil.rectOutline(ctx, minX, minY, w, h, lineWidth.getValue(), color);
            } else if (mode.is("Outline")) {
                // Corner-style outline (only draws corners, not full box)
                int cornerLen = (int) Math.max(4, Math.min(w, h) / 4);
                int lw = (int) lineWidth.getValue();
                drawCornerOutline(ctx, minX, minY, w, h, cornerLen, lw, color);
            }

            // Health bar on the left side
            if (living.getMaxHealth() > 0) {
                float healthPct = Math.min(1, living.getHealth() / living.getMaxHealth());
                int barW = 2;
                int barH = (int) h;
                int barX = (int) minX - barW - 2;
                int barY = (int) minY;
                int filledH = (int) (barH * healthPct);

                // Background
                RenderUtil.rect(ctx, barX, barY, barW, barH, 0x80000000);
                // Health fill (green to red based on health)
                int healthColor = getHealthColor(healthPct);
                RenderUtil.rect(ctx, barX, barY + barH - filledH, barW, filledH, healthColor);
            }
        }
    }

    private void drawCornerOutline(DrawContext ctx, double x, double y, double w, double h,
                                   int cornerLen, int lw, int color) {
        // Top-left
        RenderUtil.rect(ctx, x, y, cornerLen, lw, color);
        RenderUtil.rect(ctx, x, y, lw, cornerLen, color);
        // Top-right
        RenderUtil.rect(ctx, x + w - cornerLen, y, cornerLen, lw, color);
        RenderUtil.rect(ctx, x + w - lw, y, lw, cornerLen, color);
        // Bottom-left
        RenderUtil.rect(ctx, x, y + h - lw, cornerLen, lw, color);
        RenderUtil.rect(ctx, x, y + h - cornerLen, lw, cornerLen, color);
        // Bottom-right
        RenderUtil.rect(ctx, x + w - cornerLen, y + h - lw, cornerLen, lw, color);
        RenderUtil.rect(ctx, x + w - lw, y + h - cornerLen, lw, cornerLen, color);
    }

    private boolean shouldRender(Entity entity) {
        if (entity instanceof PlayerEntity && players.isEnabled()) return true;
        if (entity instanceof HostileEntity && hostileMobs.isEnabled()) return true;
        if (entity instanceof PassiveEntity && passiveMobs.isEnabled()) return true;
        return false;
    }

    private int getEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity) return COLOR_PLAYER;
        if (entity instanceof HostileEntity) return COLOR_HOSTILE;
        if (entity instanceof PassiveEntity) return COLOR_PASSIVE;
        return 0xFFFFFFFF;
    }

    private int getHealthColor(float pct) {
        int r, g;
        if (pct > 0.5f) {
            r = (int) (255 * (1.0f - pct) * 2);
            g = 255;
        } else {
            r = 255;
            g = (int) (255 * pct * 2);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    @Override
    protected void onDisable() {
        // Remove glow from all entities
        if (mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                entity.setGlowing(false);
            }
        }
    }
}
