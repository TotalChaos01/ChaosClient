package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * AntiBlind module — ported from LiquidBounce
 *
 * Protects from annoying screen effects that block your view.
 * Removes blindness, darkness, nausea, pumpkin overlay, fire overlay, etc.
 * Checked by mixins (MixinStatusEffectOverlay, MixinGameRenderer, MixinInGameHud).
 */
@ModuleInfo(name = "AntiBlind", description = "Removes annoying screen effects", category = Category.RENDER)
public class AntiBlind extends Module {

    private final BooleanSetting removeBlindness = new BooleanSetting("Remove Blindness", true);
    private final BooleanSetting removeDarkness = new BooleanSetting("Remove Darkness", true);
    private final BooleanSetting removeNausea = new BooleanSetting("Remove Nausea", true);
    private final BooleanSetting removePumpkin = new BooleanSetting("Remove Pumpkin", true);
    private final BooleanSetting removePortal = new BooleanSetting("Remove Portal", true);
    private final BooleanSetting removeWallOverlay = new BooleanSetting("Remove Wall Overlay", true);
    private final BooleanSetting removeLiquidFog = new BooleanSetting("Remove Liquid Fog", true);
    private final NumberSetting fireOpacity = new NumberSetting("Fire Opacity", 30, 0, 100, 5);
    private final BooleanSetting removeBossBar = new BooleanSetting("Remove Boss Bar", false);
    private final BooleanSetting removeTitle = new BooleanSetting("Remove Title", false);

    public AntiBlind() {
        addSettings(removeBlindness, removeDarkness, removeNausea, removePumpkin,
            removePortal, removeWallOverlay, removeLiquidFog, fireOpacity,
            removeBossBar, removeTitle);
    }

    // -- Accessors for mixin checks --

    public boolean shouldRemoveBlindness() { return isEnabled() && removeBlindness.isEnabled(); }
    public boolean shouldRemoveDarkness() { return isEnabled() && removeDarkness.isEnabled(); }
    public boolean shouldRemoveNausea() { return isEnabled() && removeNausea.isEnabled(); }
    public boolean shouldRemovePumpkin() { return isEnabled() && removePumpkin.isEnabled(); }
    public boolean shouldRemovePortal() { return isEnabled() && removePortal.isEnabled(); }
    public boolean shouldRemoveWallOverlay() { return isEnabled() && removeWallOverlay.isEnabled(); }
    public boolean shouldRemoveLiquidFog() { return isEnabled() && removeLiquidFog.isEnabled(); }
    public boolean shouldRemoveBossBar() { return isEnabled() && removeBossBar.isEnabled(); }
    public boolean shouldRemoveTitle() { return isEnabled() && removeTitle.isEnabled(); }

    /** Returns fire opacity as a float 0.0 - 1.0. If module disabled, returns 1.0. */
    public float getFireOpacityF() {
        if (!isEnabled()) return 1.0f;
        return (float) (fireOpacity.getValue() / 100.0);
    }
}
