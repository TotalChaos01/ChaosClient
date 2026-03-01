package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.attribute.EntityAttributes;

@ModuleInfo(name = "Step", description = "Allows you to step up blocks instantly", category = Category.MOVEMENT)
public class Step extends Module {

    private final NumberSetting height = new NumberSetting("Height", 1.0, 0.5, 5.0, 0.5);

    public Step() {
        addSettings(height);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT)
                .setBaseValue(height.getValue());
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT)
                    .setBaseValue(0.6);
        }
    }
}

