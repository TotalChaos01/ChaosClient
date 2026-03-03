package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.PlayerEntity;

/**
 * AutoTrap — places obsidian above and around a target player to trap them.
 */
@ModuleInfo(name = "AutoTrap", description = "Traps nearby players with obsidian", category = Category.COMBAT)
public class AutoTrap extends Module {

    private final NumberSetting range = new NumberSetting("Range", 4.5, 2.0, 6.0, 0.5);
    private final NumberSetting blocksPerTick = new NumberSetting("Blocks/Tick", 4, 1, 8, 1);
    private final BooleanSetting fullTrap = new BooleanSetting("Full Trap", true);
    private final BooleanSetting topOnly = new BooleanSetting("Top Only", false);

    public AutoTrap() {
        addSettings(range, blocksPerTick, fullTrap, topOnly);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Find nearest target
        PlayerEntity target = findTarget();
        if (target == null) return;

        int blockSlot = findObsidianSlot();
        if (blockSlot == -1) return;

        BlockPos targetPos = target.getBlockPos();
        BlockPos[] trapPositions;

        if (topOnly.isEnabled()) {
            trapPositions = new BlockPos[]{
                targetPos.up(2) // Block above head
            };
        } else if (fullTrap.isEnabled()) {
            trapPositions = new BlockPos[]{
                targetPos.up(2),          // Above head
                targetPos.north(),        // Sides at feet level
                targetPos.south(),
                targetPos.east(),
                targetPos.west(),
                targetPos.up().north(),   // Sides at head level
                targetPos.up().south(),
                targetPos.up().east(),
                targetPos.up().west()
            };
        } else {
            trapPositions = new BlockPos[]{
                targetPos.up(2),
                targetPos.north(),
                targetPos.south(),
                targetPos.east(),
                targetPos.west()
            };
        }

        int placed = 0;
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        for (BlockPos pos : trapPositions) {
            if (placed >= blocksPerTick.getValue()) break;
            if (!mc.world.getBlockState(pos).isReplaceable()) continue;
            if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.getValue() * range.getValue()) continue;

            mc.player.getInventory().setSelectedSlot(blockSlot);
            placeBlock(pos);
            placed++;
        }

        if (placed > 0) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private PlayerEntity findTarget() {
        PlayerEntity closest = null;
        double closestDist = range.getValue() + 2;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isDead()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        return closest;
    }

    private int findObsidianSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == Blocks.OBSIDIAN) return i;
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
