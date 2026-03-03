package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Highlights chests and containers through walls.
 * Uses worldToScreen projection to draw 2D outlines for block entities.
 */
@ModuleInfo(name = "ChestESP", description = "Highlights storage containers", category = Category.RENDER)
public class ChestESP extends Module {

    private final BooleanSetting chests = new BooleanSetting("Chests", true);
    private final BooleanSetting enderChests = new BooleanSetting("EnderChests", true);
    private final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);

    private static final int COLOR_CHEST = 0xFFFFAA00;
    private static final int COLOR_ENDER = 0xFF00AAFF;
    private static final int COLOR_SHULKER = 0xFFFF44FF;

    public ChestESP() {
        addSettings(chests, enderChests, shulkers, range);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.world == null || mc.player == null) return;

        DrawContext ctx = event.getDrawContext();

        int renderDist = (int) range.getValue();
        BlockPos playerPos = mc.player.getBlockPos();

        // Iterate loaded chunks for block entities (efficient)
        int chunkRadius = (renderDist >> 4) + 1;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getPos();

                    // Range check
                    double dx = pos.getX() - playerPos.getX();
                    double dy = pos.getY() - playerPos.getY();
                    double dz = pos.getZ() - playerPos.getZ();
                    if (dx * dx + dy * dy + dz * dz > renderDist * renderDist) continue;

                    int color = getBlockEntityColor(blockEntity);
                    if (color == 0) continue;

                    // Project block bounding box (1x1x1 cube) to screen
                    double bx = pos.getX();
                    double by = pos.getY();
                    double bz = pos.getZ();

                    double[][] corners = {
                        {bx, by, bz},
                        {bx + 1, by, bz},
                        {bx, by, bz + 1},
                        {bx + 1, by, bz + 1},
                        {bx, by + 1, bz},
                        {bx + 1, by + 1, bz},
                        {bx, by + 1, bz + 1},
                        {bx + 1, by + 1, bz + 1}
                    };

                    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                    double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                    boolean allVisible = true;

                    for (double[] corner : corners) {
                        double[] screen = RenderUtil.worldToScreen(corner[0], corner[1], corner[2]);
                        if (screen == null) {
                            allVisible = false;
                            break;
                        }
                        minX = Math.min(minX, screen[0]);
                        minY = Math.min(minY, screen[1]);
                        maxX = Math.max(maxX, screen[0]);
                        maxY = Math.max(maxY, screen[1]);
                    }

                    if (!allVisible) continue;

                    double w = maxX - minX;
                    double h = maxY - minY;
                    if (w < 1 || h < 1) continue;

                    // Fill with transparency
                    int fillColor = (0x30 << 24) | (color & 0x00FFFFFF);
                    RenderUtil.rect(ctx, minX, minY, w, h, fillColor);
                    // Outline
                    RenderUtil.rectOutline(ctx, minX, minY, w, h, 1, color);
                }
            }
        }
    }

    private int getBlockEntityColor(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity && chests.isEnabled()) return COLOR_CHEST;
        if (blockEntity instanceof EnderChestBlockEntity && enderChests.isEnabled()) return COLOR_ENDER;
        if (blockEntity instanceof ShulkerBoxBlockEntity && shulkers.isEnabled()) return COLOR_SHULKER;
        return 0;
    }
}
