package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * SpeedMine — increases block breaking speed.
 */
@ModuleInfo(name = "SpeedMine", description = "Increases block breaking speed", category = Category.PLAYER)
public class SpeedMine extends Module {

    private final NumberSetting speed = new NumberSetting("Modifier", 1.3, 1.0, 2.0, 0.1);

    public SpeedMine() {
        addSettings(speed);
    }

    /**
     * Returns the speed multiplier (used by mixin).
     */
    public double getSpeedMultiplier() {
        return speed.getValue();
    }
}
