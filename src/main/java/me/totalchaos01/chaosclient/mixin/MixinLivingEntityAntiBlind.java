package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.impl.render.AntiBlind;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for AntiBlind — removes blindness/darkness/nausea effects from rendering.
 * Hooks into LivingEntity.hasStatusEffect to fake the absence of effects during rendering.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityAntiBlind {

    @Inject(method = "hasStatusEffect", at = @At("RETURN"), cancellable = true)
    private void onHasStatusEffect(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValue()) return; // Only suppress if actually has the effect

            if (ChaosClient.getInstance() == null) return;
            AntiBlind antiBlind = ChaosClient.getInstance().getModuleManager().getModule(AntiBlind.class);
            if (antiBlind == null) return;

            if (effect.equals(StatusEffects.BLINDNESS) && antiBlind.shouldRemoveBlindness()) {
                cir.setReturnValue(false);
            } else if (effect.equals(StatusEffects.DARKNESS) && antiBlind.shouldRemoveDarkness()) {
                cir.setReturnValue(false);
            } else if (effect.equals(StatusEffects.NAUSEA) && antiBlind.shouldRemoveNausea()) {
                cir.setReturnValue(false);
            }
        } catch (Exception ignored) {}
    }
}
