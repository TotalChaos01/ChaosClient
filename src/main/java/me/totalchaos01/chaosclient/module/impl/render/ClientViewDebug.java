package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.player.RotationUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

/**
 * ClientView — simplified module that shows the server-side look direction
 * as a line from the player model in F5 (third person) view.
 * Useful for visualizing where KillAura/silent rotations are actually aiming.
 */
@ModuleInfo(name = "ClientView", description = "Shows server rotation direction in F5", category = Category.RENDER)
public class ClientViewDebug extends Module {

    private final NumberSetting rayLength = new NumberSetting("Ray Length", 16, 4, 48, 1);

    public ClientViewDebug() {
        addSettings(rayLength);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        DrawContext ctx = event.getDrawContext();

        // Draw server rotation ray (purple) — shows where silent aim is pointing
        if (RotationUtil.isRotating()) {
            float yaw = RotationUtil.getServerYaw();
            float pitch = RotationUtil.getServerPitch();
            Vec3d origin = mc.player.getEyePos();
            Vec3d dir = Vec3d.fromPolar(pitch, yaw).multiply(rayLength.getValue());
            Vec3d end = origin.add(dir);

            double[] s0 = RenderUtil.worldToScreen(origin.x, origin.y, origin.z);
            double[] s1 = RenderUtil.worldToScreen(end.x, end.y, end.z);
            if (s0 != null && s1 != null && s0[2] >= 0 && s0[2] <= 1 && s1[2] >= 0 && s1[2] <= 1) {
                RenderUtil.drawGlowLine(ctx, s0[0], s0[1], s1[0], s1[1], 1.5f, 0xAABF00FF);
            }
        }
    }
}
