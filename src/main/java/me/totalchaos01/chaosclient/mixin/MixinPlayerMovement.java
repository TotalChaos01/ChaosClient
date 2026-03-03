package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.Module;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into PlayerEntity for SafeWalk (clipAtLedge).
 */
@Mixin(PlayerEntity.class)
public abstract class MixinPlayerMovement {

    /**
     * SafeWalk — prevent falling off edges.
     * clipAtLedge() exists in PlayerEntity in 1.21.11.
     */
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void onClipAtLedge(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (ChaosClient.getInstance() == null || ChaosClient.getInstance().getModuleManager() == null) return;
            Module safeWalk = ChaosClient.getInstance().getModuleManager().getModule("SafeWalk");
            if (safeWalk != null && safeWalk.isEnabled()) {
                cir.setReturnValue(true);
            }
        } catch (Exception ignored) {}
    }
}
