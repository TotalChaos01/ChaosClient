package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;

/**
 * TrueSight module — ported from LiquidBounce
 *
 * Allows you to see invisible entities and barrier blocks.
 * The rendering is handled in MixinEntity (isInvisible override)
 * and MixinLivingEntityRenderer.
 */
@ModuleInfo(name = "TrueSight", description = "See invisible entities and barriers", category = Category.RENDER)
public class TrueSight extends Module {

    private final BooleanSetting seeEntities = new BooleanSetting("Entities", true);
    private final BooleanSetting seeBarriers = new BooleanSetting("Barriers", true);

    public TrueSight() {
        addSettings(seeEntities, seeBarriers);
    }

    /** Used by MixinEntity/MixinLivingEntityRenderer to check if invisible entities should be rendered. */
    public boolean shouldShowEntities() { return isEnabled() && seeEntities.isEnabled(); }

    /** Used by block rendering mixin to show barriers. */
    public boolean shouldShowBarriers() { return isEnabled() && seeBarriers.isEnabled(); }
}
