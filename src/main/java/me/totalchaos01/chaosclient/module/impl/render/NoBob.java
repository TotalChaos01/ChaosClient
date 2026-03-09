package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

/**
 * NoBob module — ported from LiquidBounce
 *
 * Disables the view bobbing effect.
 * The actual bobbing cancellation is handled in MixinBobView.
 */
@ModuleInfo(name = "NoBob", description = "Disables view bobbing", category = Category.RENDER)
public class NoBob extends Module {
    // Pure flag module — mixin checks isEnabled()
}
