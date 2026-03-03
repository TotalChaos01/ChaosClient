package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * StorageESP — highlights storage containers through walls (chests, shulkers, ender chests, barrels).
 */
@ModuleInfo(name = "StorageESP", description = "Highlights storage containers", category = Category.RENDER)
public class StorageESP extends Module {

    private final BooleanSetting chests = new BooleanSetting("Chests", true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", true);
    private final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    private final BooleanSetting barrels = new BooleanSetting("Barrels", true);
    private final NumberSetting range = new NumberSetting("Range", 64, 16, 128, 8);

    private final Set<BlockPos> storagePositions = new HashSet<>();

    public StorageESP() {
        addSettings(chests, enderChests, shulkers, barrels, range);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        storagePositions.clear();

        // Iterate loaded chunks' block entities
        var chunks = mc.world.getChunkManager();
        int viewDist = mc.options.getViewDistance().getValue();
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                var chunk = chunks.getWorldChunk(cx, cz, false);
                if (chunk == null) continue;

                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    if (be == null) continue;

                    double dist = mc.player.squaredDistanceTo(
                        be.getPos().getX() + 0.5, be.getPos().getY() + 0.5, be.getPos().getZ() + 0.5
                    );
                    if (dist > range.getValue() * range.getValue()) continue;

                    if (chests.isEnabled() && be instanceof ChestBlockEntity) {
                        storagePositions.add(be.getPos());
                    } else if (enderChests.isEnabled() && be instanceof EnderChestBlockEntity) {
                        storagePositions.add(be.getPos());
                    } else if (shulkers.isEnabled() && be instanceof ShulkerBoxBlockEntity) {
                        storagePositions.add(be.getPos());
                    } else if (barrels.isEnabled() && be instanceof BarrelBlockEntity) {
                        storagePositions.add(be.getPos());
                    }
                }
            }
        }
    }

    public Set<BlockPos> getStoragePositions() {
        return storagePositions;
    }
}
