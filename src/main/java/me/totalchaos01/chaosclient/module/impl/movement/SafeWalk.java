package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

@ModuleInfo(name = "SafeWalk", description = "Prevents you from walking off edges", category = Category.MOVEMENT)
public class SafeWalk extends Module {

    @EventTarget
    public void onTick(EventTick event) {
        // SafeWalk is implemented via mixin to prevent edge movement
        // The module just needs to be enabled for the mixin to check
    }
}

