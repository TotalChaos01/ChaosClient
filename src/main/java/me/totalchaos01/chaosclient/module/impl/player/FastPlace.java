package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

@ModuleInfo(name = "FastPlace", description = "Removes block placement delay", category = Category.PLAYER)
public class FastPlace extends Module {

    @EventTarget
    public void onTick(EventTick event) {
        // FastPlace is implemented via mixin to set itemUseCooldown to 0
    }
}

