package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks under you", category = Category.PLAYER)
public class Scaffold extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Expand");
    private final BooleanSetting tower = new BooleanSetting("Tower", false);

    public Scaffold() {
        addSettings(mode, tower);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        BlockPos below = mc.player.getBlockPos().down();
        if (!mc.world.getBlockState(below).isAir()) return;

        int blockSlot = findBlock();
        if (blockSlot == -1) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(blockSlot);

        // Place block below
        placeBlock(below);

        mc.player.getInventory().setSelectedSlot(prevSlot);

        if (tower.isEnabled() && mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
        }
    }

    private int findBlock() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem) {
                Block block = ((BlockItem) stack.getItem()).getBlock();
                if (block != Blocks.TNT && block != Blocks.SAND && block != Blocks.GRAVEL) {
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
                Vec3d hitVec = Vec3d.ofCenter(neighbor).add(
                        dir.getOpposite().getOffsetX() * 0.5,
                        dir.getOpposite().getOffsetY() * 0.5,
                        dir.getOpposite().getOffsetZ() * 0.5
                );
                BlockHitResult hit = new BlockHitResult(hitVec, dir.getOpposite(), neighbor, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}

