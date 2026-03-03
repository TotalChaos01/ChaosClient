package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * Ambience — modifies the world's time and weather visuals client-side.
 */
@ModuleInfo(name = "Ambience", description = "Customize world time and weather", category = Category.RENDER)
public class Ambience extends Module {

    private final BooleanSetting customTime = new BooleanSetting("Custom Time", true);
    private final NumberSetting time = new NumberSetting("Time", 6000, 0, 24000, 100);
    private final BooleanSetting customWeather = new BooleanSetting("Custom Weather", false);
    private final BooleanSetting rain = new BooleanSetting("Rain", false);

    public Ambience() {
        time.setVisibility(customTime::isEnabled);
        rain.setVisibility(customWeather::isEnabled);
        addSettings(customTime, time, customWeather, rain);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.world == null) return;

        if (customTime.isEnabled()) {
            // Client-side time override via world properties
            mc.world.setTime((long) time.getValue(), (long) time.getValue(), false);
        }

        if (customWeather.isEnabled()) {
            if (rain.isEnabled()) {
                mc.world.setRainGradient(1.0f);
            } else {
                mc.world.setRainGradient(0.0f);
                mc.world.setThunderGradient(0.0f);
            }
        }
    }

    @Override
    public String getSuffix() {
        return String.valueOf((int) time.getValue());
    }
}
