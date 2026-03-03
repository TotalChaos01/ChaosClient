package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

/**
 * AutoWalk — automatically walks forward.
 */
@ModuleInfo(name = "AutoWalk", description = "Automatically walks forward", category = Category.MOVEMENT)
public class AutoWalk extends Module {

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        mc.options.forwardKey.setPressed(true);
    }

    @Override
    protected void onDisable() {
        if (mc.player != null && !mc.options.forwardKey.isPressed()) {
            mc.options.forwardKey.setPressed(false);
        }
    }
}
