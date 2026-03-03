package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Burrow — places a block inside the player's own hitbox for protection.
 * The player clips into a block for extra safety in crystal PvP.
 */
@ModuleInfo(name = "Burrow", description = "Places a block inside your hitbox", category = Category.COMBAT)
public class Burrow extends Module {

    private final BooleanSetting autoDisable = new BooleanSetting("Auto Disable", true);
    private final BooleanSetting center = new BooleanSetting("Center", true);
    private final BooleanSetting rubberband = new BooleanSetting("Rubberband", true);

    public Burrow() {
        addSettings(autoDisable, center, rubberband);
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        if (!mc.player.isOnGround()) {
            toggle();
            return;
        }

        int blockSlot = findBlockSlot();
        if (blockSlot == -1) {
            toggle();
            return;
        }

        // Center player
        if (center.isEnabled()) {
            double centerX = Math.floor(mc.player.getX()) + 0.5;
            double centerZ = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPosition(centerX, mc.player.getY(), centerZ);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                centerX, mc.player.getY(), centerZ, true, mc.player.horizontalCollision
            ));
        }

        // Send packet to jump up
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
            mc.player.getX(), mc.player.getY() + 0.42, mc.player.getZ(), false, mc.player.horizontalCollision
        ));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
            mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), false, mc.player.horizontalCollision
        ));

        // Place block at feet
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(blockSlot);

        BlockPos feetPos = mc.player.getBlockPos();
        placeBlock(feetPos);

        mc.player.getInventory().setSelectedSlot(prevSlot);

        // Rubberband back down
        if (rubberband.isEnabled()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX(), mc.player.getY() - 1.01, mc.player.getZ(), true, mc.player.horizontalCollision
            ));
        }

        if (autoDisable.isEnabled()) {
            toggle();
        }
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == Blocks.OBSIDIAN || blockItem.getBlock() == Blocks.ENDER_CHEST) {
                    return i;
                }
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
