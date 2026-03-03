package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * PacketMine — mines blocks using packets for faster/instant breaking.
 * Sends start and abort mining packets to break blocks without the visible animation.
 */
@ModuleInfo(name = "PacketMine", description = "Mine blocks with packets", category = Category.PLAYER)
public class PacketMine extends Module {

    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0, 0.5);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    private final BooleanSetting swingHand = new BooleanSetting("Swing Hand", true);

    private BlockPos miningPos = null;
    private Direction miningDir = null;
    private float breakProgress = 0;

    public PacketMine() {
        addSettings(range, autoSwitch, swingHand);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (miningPos != null) {
            if (mc.player.squaredDistanceTo(
                miningPos.getX() + 0.5, miningPos.getY() + 0.5, miningPos.getZ() + 0.5
            ) > range.getValue() * range.getValue()) {
                miningPos = null;
                miningDir = null;
                breakProgress = 0;
                return;
            }

            if (mc.world.getBlockState(miningPos).isAir()) {
                miningPos = null;
                miningDir = null;
                breakProgress = 0;
                return;
            }

            // Calculate break progress
            var state = mc.world.getBlockState(miningPos);
            float hardness = state.calcBlockBreakingDelta(mc.player, mc.world, miningPos);
            breakProgress += hardness;

            if (breakProgress >= 1.0f) {
                // Block is ready to break - send finish packet
                if (autoSwitch.isEnabled()) {
                    int bestTool = findBestTool(miningPos);
                    if (bestTool != -1) {
                        mc.player.getInventory().setSelectedSlot(bestTool);
                    }
                }

                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, miningPos, miningDir
                ));

                if (swingHand.isEnabled()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                miningPos = null;
                miningDir = null;
                breakProgress = 0;
            }
        }

        // Start mining on attack key
        if (mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult blockHit) {
            BlockPos targetPos = blockHit.getBlockPos();
            if (!mc.world.getBlockState(targetPos).isAir() && !targetPos.equals(miningPos)) {
                miningPos = targetPos;
                miningDir = blockHit.getSide();
                breakProgress = 0;

                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, miningPos, miningDir
                ));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, miningPos, miningDir
                ));
            }
        }
    }

    private int findBestTool(BlockPos pos) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;

        var state = mc.world.getBlockState(pos);

        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    public BlockPos getMiningPos() {
        return miningPos;
    }

    public float getBreakProgress() {
        return breakProgress;
    }

    @Override
    protected void onDisable() {
        miningPos = null;
        miningDir = null;
        breakProgress = 0;
    }
}
