package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender3D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;

/**
 * Highlights chests and containers through walls.
 */
@ModuleInfo(name = "ChestESP", description = "Highlights storage containers", category = Category.RENDER)
public class ChestESP extends Module {

    private final BooleanSetting chests = new BooleanSetting("Chests", true);
    private final BooleanSetting enderChests = new BooleanSetting("EnderChests", true);
    private final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);

    public ChestESP() {
        addSettings(chests, enderChests, shulkers, range);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        // Iterate loaded block entities in render distance
        int renderDist = (int) range.getValue();
        int px = mc.player.getBlockPos().getX();
        int py = mc.player.getBlockPos().getY();
        int pz = mc.player.getBlockPos().getZ();

        // Scan nearby block positions for block entities
        for (int x = px - renderDist; x <= px + renderDist; x += 1) {
            for (int y = Math.max(py - renderDist, mc.world.getBottomY()); y <= Math.min(py + renderDist, mc.world.getTopYInclusive()); y += 1) {
                for (int z = pz - renderDist; z <= pz + renderDist; z += 1) {
                    BlockEntity blockEntity = mc.world.getBlockEntity(new net.minecraft.util.math.BlockPos(x, y, z));
                    if (blockEntity == null) continue;

                    if (blockEntity instanceof ChestBlockEntity && chests.isEnabled()) {
                        // Draw chest outline — GL rendering to be implemented
                    } else if (blockEntity instanceof EnderChestBlockEntity && enderChests.isEnabled()) {
                        // Draw ender chest outline
                    } else if (blockEntity instanceof ShulkerBoxBlockEntity && shulkers.isEnabled()) {
                        // Draw shulker outline
                    }
                }
            }
        }
    }
}
