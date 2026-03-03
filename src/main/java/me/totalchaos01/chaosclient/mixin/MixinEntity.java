package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.impl.render.ESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes entities glow (like spectral arrow effect) when ESP Glow mode is active.
 */
@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir.getReturnValue()) return; // Already glowing

            Entity self = (Entity) (Object) this;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || self == mc.player) return;

            ESP esp = ChaosClient.getInstance().getModuleManager().getModule(ESP.class);
            if (esp != null && esp.isEnabled() && esp.isGlowMode() && esp.isValidTarget(self)) {
                cir.setReturnValue(true);
            }
        } catch (Exception ignored) {}
    }
}
