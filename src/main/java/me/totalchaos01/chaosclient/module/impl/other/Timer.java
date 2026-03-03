package me.totalchaos01.chaosclient.module.impl.other;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Changes the game tick speed (timer speed).
 */
@ModuleInfo(name = "Timer", description = "Changes game tick speed", category = Category.EXPLOITS)
public class Timer extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", 2.0, 0.1, 10.0, 0.1);

    public Timer() {
        addSettings(speed);
    }

    @Override
    public String getSuffix() {
        return String.format("%.1f", speed.getValue());
    }

    @EventTarget
    public void onTick(EventTick event) {
        // Timer manipulation requires mixin into RenderTickCounter
        // For now this serves as a placeholder for the timer speed value
    }

    @Override
    protected void onDisable() {
        // Reset timer to normal
    }
}
