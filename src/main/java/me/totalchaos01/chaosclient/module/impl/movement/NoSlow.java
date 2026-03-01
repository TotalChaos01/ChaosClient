package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

@ModuleInfo(name = "NoSlow", description = "Prevents slowdown when using items", category = Category.MOVEMENT)
public class NoSlow extends Module {

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        // NoSlow implementation — prevents item use from slowing movement
        // Handled via mixin for item use slowdown override
    }
}

