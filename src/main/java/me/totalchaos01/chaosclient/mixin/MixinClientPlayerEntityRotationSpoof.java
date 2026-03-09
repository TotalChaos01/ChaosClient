package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.util.player.RotationUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Silent rotation spoof for movement packet sending.
 * Temporarily swaps yaw/pitch to server rotations only while movement packets are built.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntityRotationSpoof {

    @Unique private float chaos$realYaw;
    @Unique private float chaos$realPitch;
    @Unique private boolean chaos$spoofing;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void chaos$onSendMovementHead(CallbackInfo ci) {
        if (!RotationUtil.isRotating()) return;

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        chaos$realYaw = self.getYaw();
        chaos$realPitch = self.getPitch();

        self.setYaw(RotationUtil.getServerYaw());
        self.setPitch(RotationUtil.getServerPitch());
        chaos$spoofing = true;
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void chaos$onSendMovementReturn(CallbackInfo ci) {
        if (!chaos$spoofing) return;

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        self.setYaw(chaos$realYaw);
        self.setPitch(chaos$realPitch);
        chaos$spoofing = false;
    }
}

