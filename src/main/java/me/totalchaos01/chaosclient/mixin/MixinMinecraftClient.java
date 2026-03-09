package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.util.player.RotationUtil;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (ChaosClient.getInstance() == null) return;
        RotationUtil.update();
        ChaosClient.getInstance().getEventBus().post(new EventTick());
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        if (ChaosClient.getInstance() != null) {
            ChaosClient.getInstance().shutdown();
        }
    }
}
