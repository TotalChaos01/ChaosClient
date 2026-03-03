package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Surround — places obsidian around the player's feet for protection in crystal PvP.
 */
@ModuleInfo(name = "Surround", description = "Surrounds you with obsidian", category = Category.COMBAT)
public class Surround extends Module {

    private final ModeSetting blockType = new ModeSetting("Block", "Obsidian", "Obsidian", "Ender Chest", "Both");
    private final NumberSetting blocksPerTick = new NumberSetting("Blocks/Tick", 4, 1, 8, 1);
    private final BooleanSetting center = new BooleanSetting("Center", true);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only Ground", true);
    private final BooleanSetting autoDisable = new BooleanSetting("Disable on Move", true);

    private BlockPos startPos;

    public Surround() {
        addSettings(blockType, blocksPerTick, center, onlyOnGround, autoDisable);
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) {
            toggle();
            return;
        }

        startPos = mc.player.getBlockPos();

        // Center the player on the block
        if (center.isEnabled()) {
            double centerX = Math.floor(mc.player.getX()) + 0.5;
            double centerZ = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPosition(centerX, mc.player.getY(), centerZ);
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (onlyOnGround.isEnabled() && !mc.player.isOnGround()) return;

        // Check if player moved
        if (autoDisable.isEnabled() && startPos != null &&
            !mc.player.getBlockPos().equals(startPos)) {
            toggle();
            return;
        }

        // Find obsidian/ender chest slot
        int blockSlot = findBlockSlot();
        if (blockSlot == -1) return;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos[] surroundPositions = {
            playerPos.north(),
            playerPos.south(),
            playerPos.east(),
            playerPos.west(),
            // Support blocks below the side blocks
            playerPos.north().down(),
            playerPos.south().down(),
            playerPos.east().down(),
            playerPos.west().down()
        };

        int placed = 0;
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        for (BlockPos pos : surroundPositions) {
            if (placed >= blocksPerTick.getValue()) break;
            if (!mc.world.getBlockState(pos).isReplaceable()) continue;

            mc.player.getInventory().setSelectedSlot(blockSlot);
            placeBlock(pos);
            placed++;
        }

        if (placed > 0) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;

            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (blockType.is("Obsidian") && block == Blocks.OBSIDIAN) return i;
            if (blockType.is("Ender Chest") && block == Blocks.ENDER_CHEST) return i;
            if (blockType.is("Both") && (block == Blocks.OBSIDIAN || block == Blocks.ENDER_CHEST)) return i;
        }
        return -1;
    }

    private void placeBlock(BlockPos pos) {
        // Find a solid face to place against
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
