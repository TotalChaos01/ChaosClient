package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Draws tracer lines from the screen center to entities.
 * Uses 2D projected coordinates and RenderUtil.drawLine for rendering.
 */
@ModuleInfo(name = "Tracers", description = "Draws tracer lines to entities", category = Category.RENDER)
public class Tracers extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting hostileMobs = new BooleanSetting("Hostile Mobs", true);
    private final BooleanSetting passiveMobs = new BooleanSetting("Passive Mobs", false);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 256, 1);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 4, 0.5);
    private final BooleanSetting distanceColor = new BooleanSetting("Distance Color", true);

    // Colors: ARGB
    private static final int COLOR_PLAYER = 0xFFFF4444;
    private static final int COLOR_HOSTILE = 0xFFFF8844;
    private static final int COLOR_PASSIVE = 0xFF44FF44;

    public Tracers() {
        addSettings(players, hostileMobs, passiveMobs, range, lineWidth, distanceColor);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.world == null || mc.player == null) return;

        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        // Screen center (crosshair position)
        double centerX = mc.getWindow().getScaledWidth() / 2.0;
        double centerY = mc.getWindow().getScaledHeight() / 2.0;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            float dist = mc.player.distanceTo(entity);
            if (dist > range.getValue()) continue;
            if (!shouldRender(entity)) continue;

            // Get interpolated entity position (at body center height)
            Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
            double ix = lerpedPos.x;
            double iy = lerpedPos.y + entity.getHeight() / 2.0;
            double iz = lerpedPos.z;

            // Project to screen
            double[] screen = RenderUtil.worldToScreen(ix, iy, iz);
            if (screen == null) continue;

            // Determine color
            int color;
            if (distanceColor.isEnabled()) {
                color = getDistanceColor(dist, (float) range.getValue());
            } else {
                color = getEntityColor(entity);
            }

            // Draw line from screen center to entity
            RenderUtil.drawLine(ctx, centerX, centerY, screen[0], screen[1],
                    (float) lineWidth.getValue(), color);
        }
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

    /**
     * Color based on distance: green (close) -> yellow (mid) -> red (far).
     */
    private int getDistanceColor(float distance, float maxRange) {
        float pct = Math.min(1, distance / maxRange);
        int r, g;
        if (pct < 0.5f) {
            // Green to yellow
            r = (int) (255 * pct * 2);
            g = 255;
        } else {
            // Yellow to red
            r = 255;
            g = (int) (255 * (1.0f - pct) * 2);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
