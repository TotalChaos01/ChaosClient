package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * FastUse — reduces the delay between using items (eating, drinking, throwing).
 */
@ModuleInfo(name = "FastUse", description = "Removes item use cooldown", category = Category.PLAYER)
public class FastUse extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", 0, 0, 4, 1);

    public FastUse() {
        addSettings(speed);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        // Set item use cooldown to configured value
        // The MixinMinecraftClient accessor sets itemUseCooldown
        try {
            var accessor = (me.totalchaos01.chaosclient.mixin.IMinecraftClientAccessor) mc;
            accessor.setItemUseCooldown((int) speed.getValue());
        } catch (Exception ignored) {}
    }
}
