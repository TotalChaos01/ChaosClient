package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * HandView — customizes the hand/arm rendering position and scale.
 * Modifies the held item renderer transform via MixinHeldItemRenderer.
 */
@ModuleInfo(name = "HandView", description = "Customize hand view model", category = Category.RENDER)
public class HandView extends Module {

    private final NumberSetting mainX = new NumberSetting("Main X", 0.0, -2.0, 2.0, 0.05);
    private final NumberSetting mainY = new NumberSetting("Main Y", 0.0, -2.0, 2.0, 0.05);
    private final NumberSetting mainZ = new NumberSetting("Main Z", 0.0, -2.0, 2.0, 0.05);
    private final NumberSetting offX = new NumberSetting("Off X", 0.0, -2.0, 2.0, 0.05);
    private final NumberSetting offY = new NumberSetting("Off Y", 0.0, -2.0, 2.0, 0.05);
    private final NumberSetting offZ = new NumberSetting("Off Z", 0.0, -2.0, 2.0, 0.05);
    private final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.1, 2.0, 0.05);

    public HandView() {
        addSettings(mainX, mainY, mainZ, offX, offY, offZ, scale);
    }

    public float getMainX() { return (float) mainX.getValue(); }
    public float getMainY() { return (float) mainY.getValue(); }
    public float getMainZ() { return (float) mainZ.getValue(); }
    public float getOffX() { return (float) offX.getValue(); }
    public float getOffY() { return (float) offY.getValue(); }
    public float getOffZ() { return (float) offZ.getValue(); }
    public float getScale() { return (float) scale.getValue(); }
}
