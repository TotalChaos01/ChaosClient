package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.impl.render.HandView;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to modify hand/item rendering position and scale.
 * Hooks into HeldItemRenderer to apply HandView offsets.
 */
@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void onRenderFirstPersonItem(
            net.minecraft.client.network.AbstractClientPlayerEntity player,
            float tickDelta, float pitch, net.minecraft.util.Hand hand,
            float swingProgress, net.minecraft.item.ItemStack item,
            float equipProgress, MatrixStack matrices,
            OrderedRenderCommandQueue renderCommandQueue,
            int light, CallbackInfo ci) {
        try {
            if (ChaosClient.getInstance() == null) return;
            HandView handView = ChaosClient.getInstance().getModuleManager().getModule(HandView.class);
            if (handView == null || !handView.isEnabled()) return;

            Arm mainArm = player.getMainArm();
            boolean isMainHand = (hand == net.minecraft.util.Hand.MAIN_HAND);

            // Determine if this is the right or left arm rendering
            boolean isRightSide = (isMainHand && mainArm == Arm.RIGHT) || (!isMainHand && mainArm == Arm.LEFT);

            if (isRightSide) {
                matrices.translate(handView.getMainX(), handView.getMainY(), handView.getMainZ());
            } else {
                matrices.translate(handView.getOffX(), handView.getOffY(), handView.getOffZ());
            }

            float scale = handView.getScale();
            if (Math.abs(scale - 1.0f) > 0.01f) {
                matrices.scale(scale, scale, scale);
            }
        } catch (Exception ignored) {}
    }
}
