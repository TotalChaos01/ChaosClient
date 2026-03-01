package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;

@ModuleInfo(name = "Criticals", description = "Automatically land critical hits", category = Category.COMBAT)
public class Criticals extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Jump", "MiniJump");

    public Criticals() {
        addSettings(mode);
    }

    @EventTarget
    public void onTick(EventTick event) {
        // Criticals logic will hook into attack events
        // For now this serves as a placeholder for the module framework
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}

