package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.impl.render.NoSwing;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to cancel client-side swing animation when NoSwing is enabled.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntitySwing {

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"), cancellable = true)
    private void onSwingHand(Hand hand, CallbackInfo ci) {
        try {
            if (ChaosClient.getInstance() == null) return;
            NoSwing noSwing = ChaosClient.getInstance().getModuleManager().getModule(NoSwing.class);
            if (noSwing != null && noSwing.shouldHideClientSwing()) {
                LivingEntity self = (LivingEntity) (Object) this;
                if (self == net.minecraft.client.MinecraftClient.getInstance().player) {
                    ci.cancel();
                }
            }
        } catch (Exception ignored) {}
    }
}
