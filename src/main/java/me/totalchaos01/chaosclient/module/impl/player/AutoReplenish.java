package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * AutoReplenish — automatically replaces items in hotbar from inventory when they run low.
 */
@ModuleInfo(name = "AutoReplenish", description = "Automatically replenishes hotbar items", category = Category.PLAYER)
public class AutoReplenish extends Module {

    private final NumberSetting threshold = new NumberSetting("Threshold", 5, 1, 32, 1);
    private final NumberSetting delay = new NumberSetting("Delay (ticks)", 1, 0, 10, 1);

    private int tickTimer = 0;

    public AutoReplenish() {
        addSettings(threshold, delay);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.currentScreen != null) return;

        tickTimer++;
        if (tickTimer < delay.getValue()) return;
        tickTimer = 0;

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack hotbarStack = mc.player.getInventory().getStack(hotbarSlot);

            if (hotbarStack.isEmpty()) continue;
            if (!hotbarStack.isStackable()) continue;
            if (hotbarStack.getCount() > threshold.getValue()) continue;

            // Find same item in main inventory (slots 9-35)
            int replenishSlot = findItemInInventory(hotbarStack);
            if (replenishSlot != -1) {
                // Pick up from inventory and place into hotbar
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    replenishSlot, hotbarSlot,
                    SlotActionType.SWAP,
                    mc.player
                );
                return; // One swap per tick
            }
        }
    }

    private int findItemInInventory(ItemStack target) {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, target)) {
                return i;
            }
        }
        return -1;
    }
}
