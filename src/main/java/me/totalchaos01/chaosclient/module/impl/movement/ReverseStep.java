package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * ReverseStep — snaps the player down from ledges quickly instead of slowly falling.
 */
@ModuleInfo(name = "ReverseStep", description = "Snaps you down ledges quickly", category = Category.MOVEMENT)
public class ReverseStep extends Module {

    private final NumberSetting height = new NumberSetting("Height", 2.0, 0.5, 4.0, 0.5);
    private final ModeSetting mode = new ModeSetting("Mode", "Motion", "Motion", "Timer");

    public ReverseStep() {
        addSettings(mode, height);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isOnGround() && !mc.player.isSneaking() && !mc.player.isHoldingOntoLadder()) {
            // Check if there's air below within the step height
            if (!mc.player.verticalCollision) return;

            double maxY = height.getValue();
            boolean hasGround = false;

            for (double y = 0.5; y <= maxY; y += 0.5) {
                var pos = mc.player.getBlockPos().down((int) Math.ceil(y));
                if (!mc.world.getBlockState(pos).isAir()) {
                    hasGround = true;
                    break;
                }
            }

            // Check if there's air directly below
            var belowPos = mc.player.getBlockPos().down();
            if (mc.world.getBlockState(belowPos).isAir() && hasGround) {
                if (mode.is("Motion")) {
                    mc.player.setVelocity(mc.player.getVelocity().x, -height.getValue() * 0.5, mc.player.getVelocity().z);
                } else if (mode.is("Timer")) {
                    // Speed up game to make falling faster
                    mc.player.setVelocity(mc.player.getVelocity().x, -0.5, mc.player.getVelocity().z);
                }
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
