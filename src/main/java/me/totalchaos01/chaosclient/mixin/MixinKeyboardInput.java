package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.util.player.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Movement correction mixin — the heart of "silent movement".
 *
 * WHY HERE: KeyboardInput.tick() computes movementVector from keyboard state.
 * At RETURN, we rotate this vector from client-yaw space to server-yaw space.
 * When MC later uses clientYaw + rotatedInput, the result is motion toward serverYaw.
 *
 * This MUST happen here, not in EventTick. EventTick fires at HEAD of MinecraftClient.tick(),
 * but KeyboardInput.tick() runs LATER during ClientPlayerEntity.tickMovement() — which
 * overwrites any earlier modification.
 *
 * Effect: Player walks toward the target (server rotation) regardless of where the
 * camera is pointing. If camera faces opposite to target, player walks backward.
 */
@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @Inject(method = "tick", at = @At("RETURN"))
    private void chaos$afterInputTick(CallbackInfo ci) {
        if (!RotationUtil.isMoveFixActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        RotationUtil.correctMovement((Input) (Object) this, mc.player.getYaw());
    }
}
