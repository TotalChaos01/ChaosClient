package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.events.EventRender3D;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render", shift = At.Shift.AFTER), require = 0)
    private void onAfterWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
        // Capture matrices right after WorldRenderer.render while the 3D projection is still set
        float tickDelta = tickCounter.getTickProgress(true);
        RenderUtil.captureMatrices(tickDelta);
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void onRenderWorld(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ChaosClient.getInstance() == null) return;
        float tickDelta = tickCounter.getTickProgress(true);
        // Fallback capture in case the INVOKE target didn't match
        RenderUtil.captureMatrices(tickDelta);
        MatrixStack matrixStack = new MatrixStack();
        ChaosClient.getInstance().getEventBus().post(
                new EventRender3D(matrixStack, tickDelta)
        );
    }
}
