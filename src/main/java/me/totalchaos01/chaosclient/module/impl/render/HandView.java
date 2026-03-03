package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import net.minecraft.client.gui.DrawContext;

/**
 * HandView — customizes the hand/arm rendering view model.
 */
@ModuleInfo(name = "HandView", description = "Customize hand view model", category = Category.RENDER)
public class HandView extends Module {

    private final BooleanSetting noSwing = new BooleanSetting("No Swing", false);
    private final BooleanSetting smallArms = new BooleanSetting("Small Arms", false);
    private final ModeSetting swingHand = new ModeSetting("Swing Hand", "Right", "Right", "Left", "Both");

    public HandView() {
        addSettings(noSwing, smallArms, swingHand);
    }

    public boolean shouldCancelSwing() {
        return isEnabled() && noSwing.isEnabled();
    }

    public boolean isSmallArms() {
        return isEnabled() && smallArms.isEnabled();
    }
}
