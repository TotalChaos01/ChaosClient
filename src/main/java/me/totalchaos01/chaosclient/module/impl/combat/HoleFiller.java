package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * HoleFiller — fills nearby holes with blocks to prevent enemies from hiding.
 */
@ModuleInfo(name = "HoleFiller", description = "Fills holes near enemies", category = Category.COMBAT)
public class HoleFiller extends Module {

    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0, 0.5);
    private final NumberSetting blocksPerTick = new NumberSetting("Blocks/Tick", 2, 1, 8, 1);
    private final BooleanSetting onlyNearTarget = new BooleanSetting("Near Target Only", true);
    private final NumberSetting targetRange = new NumberSetting("Target Range", 8, 3, 16, 1);

    public HoleFiller() {
        targetRange.setVisibility(onlyNearTarget::isEnabled);
        addSettings(range, blocksPerTick, onlyNearTarget, targetRange);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Check if there are players nearby
        if (onlyNearTarget.isEnabled()) {
            boolean hasTarget = false;
            for (var player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (mc.player.distanceTo(player) < targetRange.getValue()) {
                    hasTarget = true;
                    break;
                }
            }
            if (!hasTarget) return;
        }

        int blockSlot = findFillBlock();
        if (blockSlot == -1) return;

        int r = (int) Math.ceil(range.getValue());
        BlockPos playerPos = mc.player.getBlockPos();
        int placed = 0;
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -2; y <= 0; y++) {
                    if (placed >= blocksPerTick.getValue()) {
                        mc.player.getInventory().setSelectedSlot(prevSlot);
                        return;
                    }

                    BlockPos pos = playerPos.add(x, y, z);
                    double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                    if (dist > range.getValue() * range.getValue()) continue;

                    // Don't fill our own position
                    if (pos.equals(mc.player.getBlockPos()) || pos.equals(mc.player.getBlockPos().down())) continue;

                    if (isHole(pos)) {
                        mc.player.getInventory().setSelectedSlot(blockSlot);
                        placeBlock(pos);
                        placed++;
                    }
                }
            }
        }

        if (placed > 0) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private boolean isHole(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;

        return isBlastResistant(pos.down()) &&
               isBlastResistant(pos.north()) &&
               isBlastResistant(pos.south()) &&
               isBlastResistant(pos.east()) &&
               isBlastResistant(pos.west());
    }

    private boolean isBlastResistant(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN ||
               block == Blocks.BEDROCK || block == Blocks.NETHERITE_BLOCK ||
               block == Blocks.REINFORCED_DEEPSLATE;
    }

    private int findFillBlock() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (block == Blocks.OBSIDIAN || block == Blocks.COBBLESTONE || block == Blocks.ENDER_CHEST) {
                return i;
            }
        }
        return -1;
    }

    private void placeBlock(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isAir()) {
                BlockHitResult hitResult = new BlockHitResult(
                    Vec3d.ofCenter(neighbor), dir.getOpposite(), neighbor, false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                return;
            }
        }
    }
}
