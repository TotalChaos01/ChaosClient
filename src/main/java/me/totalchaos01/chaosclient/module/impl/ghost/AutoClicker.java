package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.mixin.IMinecraftClientAccessor;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "AutoClicker", description = "Automatically clicks", category = Category.GHOST)
public class AutoClicker extends Module {

    private final NumberSetting minCps = new NumberSetting("Min CPS", 8, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 12, 1, 20, 1);

    private long nextClick;

    public AutoClicker() {
        addSettings(minCps, maxCps);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        if (mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) return;

        if (System.currentTimeMillis() >= nextClick) {
            // Simulate attack via keybinding press — doAttack is private
            // Using the attack key's timesPressed counter trick
            ((IMinecraftClientAccessor) mc).invokeDoAttack();
            double cps = ThreadLocalRandom.current().nextDouble(minCps.getValue(), Math.max(minCps.getValue() + 0.1, maxCps.getValue()));
            nextClick = System.currentTimeMillis() + (long) (1000.0 / cps);
        }
    }
}

