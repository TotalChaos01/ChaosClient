package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * BoatFly — allows flying while riding a boat.
 */
@ModuleInfo(name = "BoatFly", description = "Fly while in a boat", category = Category.MOVEMENT)
public class BoatFly extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", 2.0, 0.1, 10.0, 0.1);
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", 1.0, 0.1, 5.0, 0.1);

    public BoatFly() {
        addSettings(speed, verticalSpeed);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.hasVehicle()) return;

        var vehicle = mc.player.getVehicle();
        if (vehicle == null) return;

        double yaw = Math.toRadians(mc.player.getYaw());
        double spd = speed.getValue();
        double vSpd = verticalSpeed.getValue();

        double motionX = 0, motionY = 0, motionZ = 0;

        if (mc.options.forwardKey.isPressed()) {
            motionX -= Math.sin(yaw) * spd;
            motionZ += Math.cos(yaw) * spd;
        }
        if (mc.options.backKey.isPressed()) {
            motionX += Math.sin(yaw) * spd;
            motionZ -= Math.cos(yaw) * spd;
        }
        if (mc.options.jumpKey.isPressed()) motionY = vSpd;
        else if (mc.options.sneakKey.isPressed()) motionY = -vSpd;
        else motionY = 0;

        vehicle.setVelocity(motionX, motionY, motionZ);
        vehicle.setPosition(vehicle.getX() + motionX, vehicle.getY() + motionY, vehicle.getZ() + motionZ);
    }
}
