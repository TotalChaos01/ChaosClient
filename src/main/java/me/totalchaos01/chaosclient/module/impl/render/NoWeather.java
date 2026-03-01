package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

/**
 * Disables weather rendering (rain, snow, thunder).
 */
@ModuleInfo(name = "NoWeather", description = "Disables weather effects", category = Category.RENDER)
public class NoWeather extends Module {

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.world != null) {
            // Set rain/thunder to 0
            mc.world.setRainGradient(0);
            mc.world.setThunderGradient(0);
        }
    }
}
