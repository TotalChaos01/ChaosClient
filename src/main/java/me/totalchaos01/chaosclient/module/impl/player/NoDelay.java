package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.mixin.IMinecraftClientAccessor;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * NoDelay — removes various vanilla delays (block place, sword switch, jump).
 */
@ModuleInfo(name = "NoDelay", description = "Removes various game delays", category = Category.PLAYER)
public class NoDelay extends Module {

    private final BooleanSetting noPlaceDelay = new BooleanSetting("No Place Delay", true);
    private final BooleanSetting noJumpDelay = new BooleanSetting("No Jump Delay", false);
    private final NumberSetting rightClickDelay = new NumberSetting("Place Delay", 0, 0, 4, 1);

    public NoDelay() {
        addSettings(noPlaceDelay, noJumpDelay, rightClickDelay);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (noPlaceDelay.isEnabled()) {
            // Reset the right-click (block place) delay via accessor
            ((IMinecraftClientAccessor) mc).setItemUseCooldown((int) rightClickDelay.getValue());
        }

        if (noJumpDelay.isEnabled() && mc.player.isOnGround()) {
            // Use jump key to allow instant re-jump (no cooldown bypass)
            // In 1.21 jumpingCooldown is private, so we trigger jump directly
            if (mc.options.jumpKey.isPressed()) {
                mc.player.jump();
            }
        }
    }
}
