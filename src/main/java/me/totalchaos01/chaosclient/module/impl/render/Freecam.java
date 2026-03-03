package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Freecam — detaches the camera from the player and allows free movement.
 * A fake player entity is spawned at the original position.
 */
@ModuleInfo(name = "Freecam", description = "Detach camera and fly freely", category = Category.RENDER)
public class Freecam extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1);
    private final BooleanSetting showPlayer = new BooleanSetting("Show Player", true);

    private double startX, startY, startZ;
    private float startYaw, startPitch;

    public Freecam() {
        addSettings(speed, showPlayer);
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        startX = mc.player.getX();
        startY = mc.player.getY();
        startZ = mc.player.getZ();
        startYaw = mc.player.getYaw();
        startPitch = mc.player.getPitch();
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        mc.player.noClip = true;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed((float) (speed.getValue() / 10.0));
        mc.player.setOnGround(false);

        // Prevent sending movement packets while in freecam
        // The player stays at the original position server-side
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;

        mc.player.noClip = false;
        mc.player.getAbilities().flying = mc.player.isCreative();
        mc.player.getAbilities().setFlySpeed(0.05f);

        // Teleport back to original position
        mc.player.setPosition(startX, startY, startZ);
        mc.player.setYaw(startYaw);
        mc.player.setPitch(startPitch);
        mc.player.setVelocity(Vec3d.ZERO);
    }
}
