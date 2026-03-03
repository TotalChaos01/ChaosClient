package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.setting.Setting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Modifies the tick counter to change game speed when Timer module is enabled.
 */
@Mixin(RenderTickCounter.Dynamic.class)
public abstract class MixinRenderTickCounter {

    @Inject(method = "beginRenderTick(J)I", at = @At("RETURN"), cancellable = true)
    private void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        try {
            if (ChaosClient.getInstance() == null || ChaosClient.getInstance().getModuleManager() == null) return;
            Module timer = ChaosClient.getInstance().getModuleManager().getModule("Timer");
            if (timer == null || !timer.isEnabled()) return;

            // Get speed setting
            double speed = 2.0;
            for (Setting s : timer.getSettings()) {
                if (s instanceof NumberSetting ns && s.getName().equals("Speed")) {
                    speed = ns.getValue();
                    break;
                }
            }

            if (speed != 1.0) {
                int original = cir.getReturnValue();
                int modified = Math.max(1, (int) Math.round(original * speed));
                cir.setReturnValue(modified);
            }
        } catch (Exception ignored) {}
    }
}
