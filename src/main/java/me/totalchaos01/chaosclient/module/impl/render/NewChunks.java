package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.ChunkPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * NewChunks — highlights newly generated chunks with colored border lines.
 * Renders chunk boundaries projected onto screen space.
 */
@ModuleInfo(name = "NewChunks", description = "Highlights newly generated chunks", category = Category.RENDER)
public class NewChunks extends Module {

    private final NumberSetting range = new NumberSetting("Render Range", 8, 2, 16, 1);

    private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
    private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());

    public NewChunks() {
        addSettings(range);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.world == null) return;

        if (event.getPacket() instanceof ChunkDataS2CPacket packet) {
            ChunkPos chunkPos = new ChunkPos(packet.getChunkX(), packet.getChunkZ());

            if (oldChunks.contains(chunkPos)) return;

            newChunks.add(chunkPos);
            oldChunks.add(chunkPos);
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null || newChunks.isEmpty()) return;

        DrawContext ctx = event.getDrawContext();
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;
        int renderRange = (int) range.getValue();

        for (ChunkPos chunk : newChunks) {
            // Only render chunks within render range
            if (Math.abs(chunk.x - playerChunkX) > renderRange) continue;
            if (Math.abs(chunk.z - playerChunkZ) > renderRange) continue;

            double y = mc.player.getY();
            drawChunkBorder(ctx, chunk, y);
        }
    }

    private void drawChunkBorder(DrawContext ctx, ChunkPos chunk, double y) {
        double x1 = chunk.getStartX();
        double z1 = chunk.getStartZ();
        double x2 = x1 + 16;
        double z2 = z1 + 16;

        int color = 0xBB44FF44; // Green for new chunks

        // Draw the 4 edges of the chunk at player's Y level
        drawWorldLine(ctx, x1, y, z1, x2, y, z1, color);
        drawWorldLine(ctx, x2, y, z1, x2, y, z2, color);
        drawWorldLine(ctx, x2, y, z2, x1, y, z2, color);
        drawWorldLine(ctx, x1, y, z2, x1, y, z1, color);
    }

    private void drawWorldLine(DrawContext ctx, double x1, double y1, double z1,
                               double x2, double y2, double z2, int color) {
        double[] s1 = RenderUtil.worldToScreen(x1, y1, z1);
        double[] s2 = RenderUtil.worldToScreen(x2, y2, z2);
        if (s1 == null || s2 == null) return;
        if (s1[2] < 0 || s1[2] > 1 || s2[2] < 0 || s2[2] > 1) return;

        RenderUtil.drawSmoothLine(ctx, s1[0], s1[1], s2[0], s2[1], 1.5f, color);
    }

    @Override
    protected void onDisable() {
        newChunks.clear();
        oldChunks.clear();
    }
}
