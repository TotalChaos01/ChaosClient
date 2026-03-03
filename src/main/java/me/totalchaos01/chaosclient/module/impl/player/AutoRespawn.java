package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

/**
 * AutoRespawn — automatically clicks the respawn button when you die.
 */
@ModuleInfo(name = "AutoRespawn", description = "Automatically respawns on death", category = Category.PLAYER)
public class AutoRespawn extends Module {

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (mc.player.isDead()) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
