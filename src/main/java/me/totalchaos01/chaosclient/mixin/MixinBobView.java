package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.impl.render.NoBob;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to cancel view bobbing when NoBob is enabled.
 */
@Mixin(GameRenderer.class)
public class MixinBobView {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        try {
            if (ChaosClient.getInstance() == null) return;
            NoBob noBob = ChaosClient.getInstance().getModuleManager().getModule(NoBob.class);
            if (noBob != null && noBob.isEnabled()) {
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }
}
