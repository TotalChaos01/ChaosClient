package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * Zoom — lowers FOV for a scoped-in effect. Use Hold bind mode for best experience.
 */
@ModuleInfo(name = "Zoom", description = "Zooms in the camera", category = Category.PLAYER)
public class Zoom extends Module {

    private final NumberSetting fov = new NumberSetting("FOV", 20, 5, 60, 1);
    private final BooleanSetting smooth = new BooleanSetting("Smooth", true);

    private double savedFov;
    private double currentFov;

    public Zoom() {
        setHoldMode(true); // Default to hold mode for zoom
        addSettings(fov, smooth);
    }

    @Override
    protected void onEnable() {
        savedFov = mc.options.getFov().getValue();
        currentFov = savedFov;
    }

    @Override
    protected void onDisable() {
        mc.options.getFov().setValue((int) savedFov);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        double target = fov.getValue();
        if (smooth.isEnabled()) {
            currentFov += (target - currentFov) * 0.3;
        } else {
            currentFov = target;
        }
        mc.options.getFov().setValue((int) currentFov);
    }
}
