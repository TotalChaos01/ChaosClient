package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

/**
 * AntiAnvil — prevents anvils from landing on you by stepping out of the way.
 */
@ModuleInfo(name = "AntiAnvil", description = "Prevents anvils from landing on you", category = Category.MOVEMENT)
public class AntiAnvil extends Module {

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Check blocks above the player for falling anvils
        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (entity instanceof net.minecraft.entity.FallingBlockEntity fallingBlock) {
                if (fallingBlock.getBlockState().getBlock() == net.minecraft.block.Blocks.ANVIL ||
                    fallingBlock.getBlockState().getBlock() == net.minecraft.block.Blocks.CHIPPED_ANVIL ||
                    fallingBlock.getBlockState().getBlock() == net.minecraft.block.Blocks.DAMAGED_ANVIL) {

                    double dx = mc.player.getX() - fallingBlock.getX();
                    double dz = mc.player.getZ() - fallingBlock.getZ();
                    double horizontalDist = Math.sqrt(dx * dx + dz * dz);

                    // If the anvil is falling near us
                    if (horizontalDist < 1.5 && fallingBlock.getY() > mc.player.getY()) {
                        // Move away from the anvil
                        double angle = Math.atan2(dz, dx);
                        mc.player.setVelocity(Math.cos(angle) * 0.5, mc.player.getVelocity().y, Math.sin(angle) * 0.5);
                    }
                }
            }
        }
    }
}
