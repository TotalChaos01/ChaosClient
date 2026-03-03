package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.util.math.BlockPos;

/**
 * Anchor — makes you snap into holes (1x1 bedrock/obsidian surrounded areas).
 * Used in crystal PvP to quickly get into safe holes.
 */
@ModuleInfo(name = "Anchor", description = "Snaps you into nearby holes", category = Category.MOVEMENT)
public class Anchor extends Module {

    private final NumberSetting range = new NumberSetting("Range", 3.0, 1.0, 5.0, 0.5);
    private final NumberSetting pullSpeed = new NumberSetting("Pull Speed", 0.3, 0.1, 1.0, 0.05);

    public Anchor() {
        addSettings(range, pullSpeed);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isOnGround()) return; // Only when falling

        BlockPos bestHole = findNearestHole();
        if (bestHole == null) return;

        double dx = bestHole.getX() + 0.5 - mc.player.getX();
        double dz = bestHole.getZ() + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.1) {
            double factor = pullSpeed.getValue() / dist;
            mc.player.setVelocity(dx * factor, mc.player.getVelocity().y, dz * factor);
        }
    }

    private BlockPos findNearestHole() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestPos = null;
        double bestDist = range.getValue();

        int r = (int) Math.ceil(range.getValue());
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -3; y <= 0; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (isHole(pos)) {
                        double dist = Math.sqrt(
                            Math.pow(mc.player.getX() - (pos.getX() + 0.5), 2) +
                            Math.pow(mc.player.getY() - pos.getY(), 2) +
                            Math.pow(mc.player.getZ() - (pos.getZ() + 0.5), 2)
                        );
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestPos = pos;
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    private boolean isHole(BlockPos pos) {
        // A hole: air at pos level, surrounded by blast-resistant blocks on all 4 sides + bottom
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;

        return isBlastResistant(pos.down()) &&
               isBlastResistant(pos.north()) &&
               isBlastResistant(pos.south()) &&
               isBlastResistant(pos.east()) &&
               isBlastResistant(pos.west());
    }

    private boolean isBlastResistant(BlockPos pos) {
        var block = mc.world.getBlockState(pos).getBlock();
        return block == net.minecraft.block.Blocks.OBSIDIAN ||
               block == net.minecraft.block.Blocks.CRYING_OBSIDIAN ||
               block == net.minecraft.block.Blocks.BEDROCK ||
               block == net.minecraft.block.Blocks.NETHERITE_BLOCK ||
               block == net.minecraft.block.Blocks.REINFORCED_DEEPSLATE;
    }
}
