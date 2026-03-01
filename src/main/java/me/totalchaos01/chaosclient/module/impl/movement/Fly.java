package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

@ModuleInfo(name = "Fly", description = "Allows you to fly", category = Category.MOVEMENT)
public class Fly extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Creative");
    private final NumberSetting speed = new NumberSetting("Speed", 2.0, 0.1, 10.0, 0.1);

    public Fly() {
        addSettings(mode, speed);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        switch (mode.getMode()) {
            case "Vanilla" -> {
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().setFlySpeed((float) (speed.getValue() / 20.0));
            }
            case "Creative" -> {
                mc.player.getAbilities().allowFlying = true;
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().setFlySpeed((float) (speed.getValue() / 20.0));
            }
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().allowFlying = mc.player.isCreative();
        mc.player.getAbilities().setFlySpeed(0.05f);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}

