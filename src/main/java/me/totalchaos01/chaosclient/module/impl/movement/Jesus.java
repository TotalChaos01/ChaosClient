package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.WaterFluid;

@ModuleInfo(name = "Jesus", description = "Walk on water and lava", category = Category.MOVEMENT)
public class Jesus extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Solid", "Solid", "Dolphin");

    public Jesus() {
        addSettings(mode);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Solid")) {
            if (mc.player.isTouchingWater() && !mc.player.isSneaking()) {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
            }
        } else if (mode.is("Dolphin")) {
            if (mc.player.isTouchingWater()) {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.04, mc.player.getVelocity().z);
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}

