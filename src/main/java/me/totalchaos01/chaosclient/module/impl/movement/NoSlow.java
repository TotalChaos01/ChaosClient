package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/**
 * NoSlow — prevents item-use and block slowdown.
 * Modes: Vanilla (direct), Grim (packet bypass), Matrix (interact bypass).
 * Configurable: food, bows, shields, honey/soul sand blocks.
 */
@ModuleInfo(name = "NoSlow", description = "Prevents slowdown when using items or on blocks", category = Category.MOVEMENT)
public class NoSlow extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Grim", "Matrix");
    private final BooleanSetting food = new BooleanSetting("Food", true);
    private final BooleanSetting bows = new BooleanSetting("Bows", true);
    private final BooleanSetting shields = new BooleanSetting("Shields", true);
    private final BooleanSetting potions = new BooleanSetting("Potions", true);
    private final BooleanSetting honeyBlock = new BooleanSetting("Honey Block", true);
    private final BooleanSetting soulSand = new BooleanSetting("Soul Sand", true);
    private final BooleanSetting cobweb = new BooleanSetting("Cobwebs", false);

    public NoSlow() {
        addSettings(mode, food, bows, shields, potions, honeyBlock, soulSand, cobweb);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Item use slowdown bypass
        if (mc.player.isUsingItem() && shouldPreventItemSlow()) {
            switch (mode.getMode()) {
                case "Grim" -> {
                    int currentSlot = mc.player.getInventory().getSelectedSlot();
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
                }
                case "Matrix" -> {
                    mc.player.networkHandler.sendPacket(
                            new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                }
            }
        }

        // Block slowdown bypass: honey, soul sand, cobweb — handled by overriding velocity
        if (shouldPreventBlockSlow()) {
            BlockPos below = mc.player.getBlockPos();
            BlockPos feetPos = mc.player.getBlockPos();
            BlockState belowState = mc.world.getBlockState(below.down());
            BlockState feetState = mc.world.getBlockState(feetPos);

            boolean onHoney = honeyBlock.isEnabled() && belowState.isOf(Blocks.HONEY_BLOCK);
            boolean onSoulSand = soulSand.isEnabled() && belowState.isOf(Blocks.SOUL_SAND);
            boolean inCobweb = cobweb.isEnabled() && feetState.isOf(Blocks.COBWEB);

            if (onHoney || onSoulSand) {
                // Compensate for slowdown by boosting movement
                double speed = 0.113; // normal walk speed
                if (mc.player.isSprinting()) speed = 0.147;
                double yaw = Math.toRadians(mc.player.getYaw());
                var pi = mc.player.input.playerInput;
                double forward = (pi.forward() ? 1 : 0) - (pi.backward() ? 1 : 0);
                double strafe = (pi.left() ? 1 : 0) - (pi.right() ? 1 : 0);
                if (forward != 0 || strafe != 0) {
                    double mag = Math.sqrt(forward * forward + strafe * strafe);
                    forward /= mag;
                    strafe /= mag;
                    double motX = forward * speed * -Math.sin(yaw) + strafe * speed * Math.cos(yaw);
                    double motZ = forward * speed * Math.cos(yaw) + strafe * speed * Math.sin(yaw);
                    mc.player.setVelocity(motX, mc.player.getVelocity().y, motZ);
                }
            }
            if (inCobweb) {
                mc.player.setVelocity(
                        mc.player.getVelocity().x * 3.0,
                        mc.player.getVelocity().y,
                        mc.player.getVelocity().z * 3.0);
            }
        }
    }

    /** Check if the currently held item type should have its slowdown prevented. */
    private boolean shouldPreventItemSlow() {
        if (mc.player == null) return false;
        ItemStack using = mc.player.getActiveItem();
        if (using.isEmpty()) return false;
        Item item = using.getItem();

        if (using.getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD) && food.isEnabled()) return true;
        if (item instanceof BowItem && bows.isEnabled()) return true;
        if (item instanceof ShieldItem && shields.isEnabled()) return true;
        if (item instanceof PotionItem && potions.isEnabled()) return true;
        if (item instanceof CrossbowItem && bows.isEnabled()) return true;
        if (item instanceof TridentItem && bows.isEnabled()) return true;

        return false;
    }

    private boolean shouldPreventBlockSlow() {
        return honeyBlock.isEnabled() || soulSand.isEnabled() || cobweb.isEnabled();
    }

    /** Called by mixin to check if slowdown should be prevented. */
    public boolean shouldPreventSlow() {
        return isEnabled() && (mc.player != null && mc.player.isUsingItem() && shouldPreventItemSlow());
    }
}
