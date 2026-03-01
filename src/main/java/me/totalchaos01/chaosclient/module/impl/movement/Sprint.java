package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;

@ModuleInfo(name = "Sprint", description = "Automatically sprints", category = Category.MOVEMENT)
public class Sprint extends Module {

    private final BooleanSetting omnidirectional = new BooleanSetting("Omnidirectional", false);

    public Sprint() {
        addSettings(omnidirectional);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (omnidirectional.isEnabled()) {
            var input = mc.player.input.playerInput;
            mc.player.setSprinting(input.forward() || input.backward() || input.left() || input.right());
        } else {
            if (mc.player.input.playerInput.forward()) {
                mc.player.setSprinting(true);
            }
        }
    }
}

