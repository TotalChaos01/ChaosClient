package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.impl.render.XRAY;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * XRAY mixin — controls which block faces render.
 * When XRAY is enabled:
 *   - Visible blocks (ores, chests) → force all faces to render
 *   - Non-visible blocks (stone, dirt) → hide all faces
 */
@Mixin(Block.class)
public abstract class MixinBlockXray {

    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockState otherState, Direction side,
                                         CallbackInfoReturnable<Boolean> cir) {
        try {
            if (ChaosClient.getInstance() == null) return;
            XRAY xray = ChaosClient.getInstance().getModuleManager().getModule(XRAY.class);
            if (xray != null && xray.isEnabled()) {
                cir.setReturnValue(xray.isVisibleBlock(state.getBlock()));
            }
        } catch (Exception ignored) {}
    }
}
