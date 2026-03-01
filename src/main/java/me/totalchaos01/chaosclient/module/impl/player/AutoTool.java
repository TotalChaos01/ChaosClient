package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

@ModuleInfo(name = "AutoTool", description = "Automatically selects the best tool", category = Category.PLAYER)
public class AutoTool extends Module {

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.crosshairTarget == null) return;

        if (mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
            var blockState = mc.world.getBlockState(blockHit.getBlockPos());
            if (blockState.isAir()) return;

            float bestSpeed = 1.0f;
            int bestSlot = mc.player.getInventory().getSelectedSlot();

            for (int i = 0; i < 9; i++) {
                var stack = mc.player.getInventory().getStack(i);
                float speed = stack.getMiningSpeedMultiplier(blockState);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }

            if (bestSlot != mc.player.getInventory().getSelectedSlot()) {
                mc.player.getInventory().setSelectedSlot(bestSlot);
            }
        }
    }
}

