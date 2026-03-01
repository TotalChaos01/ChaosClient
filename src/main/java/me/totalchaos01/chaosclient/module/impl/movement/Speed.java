package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

@ModuleInfo(name = "Speed", description = "Increases movement speed", category = Category.MOVEMENT)
public class Speed extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Strafe");
    private final NumberSetting speed = new NumberSetting("Speed", 1.5, 0.1, 5.0, 0.1);

    public Speed() {
        addSettings(mode, speed);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        if (!mc.player.isOnGround()) return;

        if (mode.is("Vanilla")) {
            double motionX = mc.player.getVelocity().x;
            double motionZ = mc.player.getVelocity().z;
            double currentSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (currentSpeed > 0.01) {
                double factor = speed.getValue();
                mc.player.setVelocity(motionX * factor, mc.player.getVelocity().y, motionZ * factor);
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}

