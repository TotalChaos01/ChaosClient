package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Extends attack reach distance.
 */
@ModuleInfo(name = "Reach", description = "Extends attack reach distance", category = Category.GHOST)
public class Reach extends Module {

    private final NumberSetting range = new NumberSetting("Range", 3.5, 3.0, 6.0, 0.1);

    public Reach() {
        addSettings(range);
    }

    public double getReach() {
        return isEnabled() ? range.getValue() : 3.0;
    }

    @EventTarget
    public void onTick(EventTick event) {
        // Reach is applied via mixin or access to interaction manager
        // This tick handler can be used for visual feedback if needed
    }
}
