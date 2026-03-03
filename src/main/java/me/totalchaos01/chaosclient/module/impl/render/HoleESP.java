package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * HoleESP — highlights safe holes for crystal PvP (1x1 areas surrounded by blast-resistant blocks).
 */
@ModuleInfo(name = "HoleESP", description = "Highlights safe holes for crystal PvP", category = Category.RENDER)
public class HoleESP extends Module {

    private final NumberSetting range = new NumberSetting("Range", 10, 3, 20, 1);
    private final BooleanSetting bedrockOnly = new BooleanSetting("Bedrock Only", false);
    private final BooleanSetting doubleHoles = new BooleanSetting("Double Holes", true);

    private final Set<BlockPos> holes = new HashSet<>();

    public HoleESP() {
        addSettings(range, bedrockOnly, doubleHoles);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        holes.clear();
        int r = (int) range.getValue();
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -3; y <= 3; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (isHole(pos)) {
                        holes.add(pos);
                    }
                }
            }
        }
    }

    private boolean isHole(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;

        BlockPos down = pos.down();
        BlockPos north = pos.north();
        BlockPos south = pos.south();
        BlockPos east = pos.east();
        BlockPos west = pos.west();

        if (bedrockOnly.isEnabled()) {
            return isBedrock(down) && isBedrock(north) && isBedrock(south) &&
                   isBedrock(east) && isBedrock(west);
        } else {
            return isBlastResistant(down) && isBlastResistant(north) && isBlastResistant(south) &&
                   isBlastResistant(east) && isBlastResistant(west);
        }
    }

    private boolean isBlastResistant(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN ||
               block == Blocks.BEDROCK || block == Blocks.NETHERITE_BLOCK ||
               block == Blocks.REINFORCED_DEEPSLATE;
    }

    private boolean isBedrock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    /**
     * Returns the set of detected holes for rendering by the Render3D event handler.
     */
    public Set<BlockPos> getHoles() {
        return holes;
    }

    /**
     * Returns whether a hole is bedrock-only (true) or mixed (false).
     */
    public boolean isBedrockHole(BlockPos pos) {
        return isBedrock(pos.down()) && isBedrock(pos.north()) &&
               isBedrock(pos.south()) && isBedrock(pos.east()) && isBedrock(pos.west());
    }
}
