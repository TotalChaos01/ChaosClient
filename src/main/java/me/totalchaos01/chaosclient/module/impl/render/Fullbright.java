package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * Makes the world fully bright regardless of light level.
 */
@ModuleInfo(name = "Fullbright", description = "Maximum brightness", category = Category.RENDER)
public class Fullbright extends Module {

    private final NumberSetting gamma = new NumberSetting("Gamma", 16.0, 1.0, 16.0, 0.5);

    private double previousGamma;

    public Fullbright() {
        addSettings(gamma);
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) {
            previousGamma = mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(gamma.getValue());
        }
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) {
            mc.options.getGamma().setValue(previousGamma);
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.options != null) {
            mc.options.getGamma().setValue(gamma.getValue());
        }
    }
}
