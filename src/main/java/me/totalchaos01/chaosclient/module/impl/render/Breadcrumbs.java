package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayDeque;

/**
 * Breadcrumbs module — ported from LiquidBounce
 *
 * Leaves a visible trail behind the player showing their movement path.
 * Renders as 2D projected lines with configurable lifetime and fade.
 */
@ModuleInfo(name = "Breadcrumbs", description = "Leaves a trail behind you", category = Category.RENDER)
public class Breadcrumbs extends Module {

    private final NumberSetting red = new NumberSetting("Red", 70, 0, 255, 1);
    private final NumberSetting green = new NumberSetting("Green", 119, 0, 255, 1);
    private final NumberSetting blue = new NumberSetting("Blue", 255, 0, 255, 1);
    private final NumberSetting alpha = new NumberSetting("Alpha", 180, 0, 255, 1);
    private final NumberSetting aliveTime = new NumberSetting("Alive Time", 3000, 500, 15000, 100);
    private final BooleanSetting fade = new BooleanSetting("Fade", true);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 5.0, 0.1);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", false);

    private final ArrayDeque<TrailPoint> trail = new ArrayDeque<>();
    private double lastX, lastY, lastZ;
    private boolean hasLastPos = false;

    public Breadcrumbs() {
        addSettings(red, green, blue, alpha, aliveTime, fade, lineWidth, rainbow);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        long maxAge = (long) aliveTime.getValue();

        // Remove expired trail points
        while (!trail.isEmpty() && now - trail.peekFirst().time > maxAge) {
            trail.removeFirst();
        }

        // Add new position if player moved
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        if (!hasLastPos || distSq(px, py, pz, lastX, lastY, lastZ) > 0.001) {
            trail.addLast(new TrailPoint(px, py, pz, now));
            lastX = px;
            lastY = py;
            lastZ = pz;
            hasLastPos = true;
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || trail.size() < 2) return;

        DrawContext ctx = event.getDrawContext();
        long now = System.currentTimeMillis();
        long maxAge = (long) aliveTime.getValue();
        float baseAlpha = (float) (alpha.getValue() / 255.0);

        TrailPoint prev = null;
        for (TrailPoint point : trail) {
            if (prev != null) {
                double[] screen1 = RenderUtil.worldToScreen(prev.x, prev.y, prev.z);
                double[] screen2 = RenderUtil.worldToScreen(point.x, point.y, point.z);

                if (screen1 != null && screen2 != null && screen1[2] > 0 && screen1[2] < 1
                        && screen2[2] > 0 && screen2[2] < 1) {
                    float a = computeAlpha(point, now, maxAge, baseAlpha);
                    int color = getColor(a);
                    RenderUtil.drawSmoothLine(ctx, screen1[0], screen1[1], screen2[0], screen2[1],
                        (float) lineWidth.getValue(), color);
                }
            }
            prev = point;
        }

        // Connect last trail point to current player position
        if (prev != null) {
            double[] screenPrev = RenderUtil.worldToScreen(prev.x, prev.y, prev.z);
            double[] screenNow = RenderUtil.worldToScreen(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (screenPrev != null && screenNow != null && screenPrev[2] > 0 && screenPrev[2] < 1
                    && screenNow[2] > 0 && screenNow[2] < 1) {
                int color = getColor(baseAlpha);
                RenderUtil.drawSmoothLine(ctx, screenPrev[0], screenPrev[1], screenNow[0], screenNow[1],
                    (float) lineWidth.getValue(), color);
            }
        }
    }

    private int getColor(float a) {
        int r, g, b;
        if (rainbow.isEnabled()) {
            float hue = (System.currentTimeMillis() % 3600) / 3600.0f;
            int rgb = Color.HSBtoRGB(hue, 0.9f, 1.0f);
            r = (rgb >> 16) & 0xFF;
            g = (rgb >> 8) & 0xFF;
            b = rgb & 0xFF;
        } else {
            r = (int) red.getValue();
            g = (int) green.getValue();
            b = (int) blue.getValue();
        }
        int ai = (int) (a * 255);
        return (ai << 24) | (r << 16) | (g << 8) | b;
    }

    private float computeAlpha(TrailPoint point, long now, long maxAge, float baseAlpha) {
        if (!fade.isEnabled()) return baseAlpha;
        float age = (float) (now - point.time);
        float ratio = 1.0f - (age / (float) maxAge);
        return Math.max(0, ratio * baseAlpha);
    }

    private double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2, dy = y1 - y2, dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    protected void onEnable() {
        trail.clear();
        hasLastPos = false;
    }

    @Override
    protected void onDisable() {
        trail.clear();
        hasLastPos = false;
    }

    private record TrailPoint(double x, double y, double z, long time) {}
}
