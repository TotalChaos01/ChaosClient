package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.events.EventRender3D;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void onRenderWorld(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ChaosClient.getInstance() == null) return;
        MatrixStack matrixStack = new MatrixStack();
        ChaosClient.getInstance().getEventBus().post(
                new EventRender3D(matrixStack, tickCounter.getTickProgress(true))
        );
    }
}
