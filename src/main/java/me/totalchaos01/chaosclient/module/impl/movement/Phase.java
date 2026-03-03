package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * Phase — allows the player to pass through solid blocks.
 */
@ModuleInfo(name = "Phase", description = "Phase through solid blocks", category = Category.MOVEMENT)
public class Phase extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "NCP");
    private final NumberSetting speed = new NumberSetting("Speed", 0.1, 0.01, 1.0, 0.01);

    public Phase() {
        addSettings(mode, speed);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Vanilla")) {
            mc.player.noClip = true;
            mc.player.getAbilities().flying = false;
            mc.player.setOnGround(false);

            double yaw = Math.toRadians(mc.player.getYaw());
            double spd = speed.getValue();

            double motionX = 0, motionY = 0, motionZ = 0;

            if (mc.options.forwardKey.isPressed()) {
                motionX -= Math.sin(yaw) * spd;
                motionZ += Math.cos(yaw) * spd;
            }
            if (mc.options.backKey.isPressed()) {
                motionX += Math.sin(yaw) * spd;
                motionZ -= Math.cos(yaw) * spd;
            }
            if (mc.options.leftKey.isPressed()) {
                motionX += Math.cos(yaw) * spd;
                motionZ += Math.sin(yaw) * spd;
            }
            if (mc.options.rightKey.isPressed()) {
                motionX -= Math.cos(yaw) * spd;
                motionZ -= Math.sin(yaw) * spd;
            }
            if (mc.options.jumpKey.isPressed()) motionY = spd;
            if (mc.options.sneakKey.isPressed()) motionY = -spd;

            mc.player.setVelocity(motionX, motionY, motionZ);
        } else if (mode.is("NCP")) {
            // NCP bypass: move into block by small amounts
            if (mc.player.horizontalCollision) {
                double yaw = Math.toRadians(mc.player.getYaw());
                mc.player.setVelocity(
                    -Math.sin(yaw) * speed.getValue() * 0.5,
                    mc.player.getVelocity().y,
                    Math.cos(yaw) * speed.getValue() * 0.5
                );
                mc.player.noClip = true;
            }
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            mc.player.noClip = false;
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
