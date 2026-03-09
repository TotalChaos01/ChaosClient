package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Maximum brightness via multiple methods.
 * Night Vision: permanent effect (reliable, works with most shaders).
 * Gamma: sets raw gamma value bypassing MC's 0-1 validation.
 * Both: applies both methods simultaneously for maximum brightness.
 */
@ModuleInfo(name = "Fullbright", description = "Maximum brightness", category = Category.RENDER)
public class Fullbright extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Both", "Night Vision", "Gamma", "Both");
    private double previousGamma = 1.0;

    public Fullbright() {
        addSettings(mode);
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) {
            previousGamma = mc.options.getGamma().getValue();
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (mc.options != null) {
            setGammaRaw(previousGamma);
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        String m = mode.getMode();

        if ("Night Vision".equals(m) || "Both".equals(m)) {
            if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, 999999, 0, false, false, false));
            }
        }

        if ("Gamma".equals(m) || "Both".equals(m)) {
            // Bypass MC's gamma validation (max 1.0 in 1.21.11) via raw accessor
            setGammaRaw(100.0);
        }
    }

    /**
     * Bypasses SimpleOption validation by setting the raw field value directly.
     * MC 1.21.11 rejects gamma > 1.0 via setValue(), causing ERROR spam.
     */
    /**
     * Directly writes to SimpleOption.value via access widener.
     * Bypasses validation that caps gamma at 1.0 in MC 1.21.11.
     */
    private void setGammaRaw(double value) {
        try {
            mc.options.getGamma().value = value;
        } catch (Exception ignored) {
            try { mc.options.getGamma().setValue(1.0); } catch (Exception ignored2) {}
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
