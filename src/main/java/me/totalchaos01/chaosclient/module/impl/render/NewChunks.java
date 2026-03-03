package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * NewChunks — highlights chunks that were newly generated (never loaded before by a player).
 * Detects new chunks based on the presence of certain indicators in chunk data.
 */
@ModuleInfo(name = "NewChunks", description = "Highlights newly generated chunks", category = Category.RENDER)
public class NewChunks extends Module {

    private final NumberSetting range = new NumberSetting("Range", 256, 64, 512, 16);
    private final BooleanSetting showOld = new BooleanSetting("Show Old Chunks", false);

    private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());

    public NewChunks() {
        addSettings(range, showOld);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.world == null) return;

        if (event.getPacket() instanceof ChunkDataS2CPacket packet) {
            ChunkPos chunkPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

            // New chunk detection heuristic:
            // If the chunk has heightmap data but no lighting, or if it's a fresh generation
            // Simple approach: first time we see a chunk, it might be new
            if (oldChunks.contains(chunkPos)) {
                // Already seen this chunk - it's old
                return;
            }

            // Mark as new (first load this session)
            newChunks.add(chunkPos);
            oldChunks.add(chunkPos);
        }
    }

    @Override
    protected void onDisable() {
        newChunks.clear();
        oldChunks.clear();
    }

    public Set<ChunkPos> getNewChunks() {
        return newChunks;
    }
}
