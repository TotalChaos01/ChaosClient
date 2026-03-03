package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

import java.awt.Color;

/**
 * Chams — renders entities with colored overlays visible through walls.
 * Requires a mixin to modify entity rendering pipeline.
 */
@ModuleInfo(name = "Chams", description = "See entities through walls with color", category = Category.RENDER)
public class Chams extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting crystals = new BooleanSetting("Crystals", true);
    private final NumberSetting alpha = new NumberSetting("Alpha", 0.6, 0.1, 1.0, 0.1);

    public Chams() {
        addSettings(players, mobs, crystals, alpha);
    }

    public boolean shouldRenderPlayer() { return isEnabled() && players.isEnabled(); }
    public boolean shouldRenderMob() { return isEnabled() && mobs.isEnabled(); }
    public boolean shouldRenderCrystal() { return isEnabled() && crystals.isEnabled(); }
    public float getAlpha() { return (float) alpha.getValue(); }
}
