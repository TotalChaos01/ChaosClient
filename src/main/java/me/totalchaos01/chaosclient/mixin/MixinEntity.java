package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.impl.ghost.Hitbox;
import me.totalchaos01.chaosclient.module.impl.ghost.Reach;
import me.totalchaos01.chaosclient.module.impl.render.ESP;
import me.totalchaos01.chaosclient.module.impl.render.ItemESP;
import me.totalchaos01.chaosclient.module.impl.render.TrueSight;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity hooks:
 * - ESP glow mode support
 * - Legit hitbox/reach target margin extension
 * - Bounding box expansion for visible F3+B hitboxes
 */
@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir.getReturnValue()) return;

            Entity self = (Entity) (Object) this;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || self == mc.player) return;

            ESP esp = ChaosClient.getInstance().getModuleManager().getModule(ESP.class);
            if (esp != null && esp.isEnabled() && esp.isGlowMode() && esp.isValidTarget(self)) {
                cir.setReturnValue(true);
                return;
            }

            // ItemESP glow
            ItemESP itemEsp = ChaosClient.getInstance().getModuleManager().getModule(ItemESP.class);
            if (itemEsp != null && itemEsp.shouldGlow(self)) {
                cir.setReturnValue(true);
                return;
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
    private void onIsInvisible(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValue()) return; // Only override invisible entities

            Entity self = (Entity) (Object) this;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || self == mc.player) return;

            TrueSight trueSight = ChaosClient.getInstance().getModuleManager().getModule(TrueSight.class);
            if (trueSight != null && trueSight.shouldShowEntities()) {
                cir.setReturnValue(false);
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        try {
            Entity self = (Entity) (Object) this;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || self == mc.player) return;
            if (!(self instanceof LivingEntity)) return;

            float extra = 0.0f;

            Hitbox hitbox = Hitbox.get();
            if (hitbox != null && hitbox.isEnabled()) {
                extra += hitbox.getSize();
            }

            Reach reach = Reach.get();
            if (reach != null && reach.isEnabled()) {
                extra += reach.getValue() * 0.30f;
            }

            if (extra > 0.0f) {
                cir.setReturnValue(cir.getReturnValue() + extra);
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<Box> cir) {
        try {
            Entity self = (Entity) (Object) this;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || self == mc.player) return;
            if (!(self instanceof LivingEntity)) return;

            Hitbox hitbox = Hitbox.get();
            if (hitbox == null || !hitbox.isEnabled()) return;

            float expand = hitbox.getSize();
            if (expand <= 0.0f) return;

            cir.setReturnValue(cir.getReturnValue().expand(expand));
        } catch (Exception ignored) {}
    }
}
