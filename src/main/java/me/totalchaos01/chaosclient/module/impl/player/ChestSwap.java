package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * ChestSwap — quickly swaps all armor with items in inventory or vice versa.
 * Useful for quickly equipping/removing full armor sets.
 */
@ModuleInfo(name = "ChestSwap", description = "Swaps armor set to/from inventory", category = Category.PLAYER)
public class ChestSwap extends Module {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    @Override
    protected void onEnable() {
        if (mc.player == null) {
            toggle();
            return;
        }

        // Check if we have armor equipped
        boolean hasArmor = false;
        for (int i = 0; i < 4; i++) {
            if (!mc.player.getEquippedStack(ARMOR_SLOTS[i]).isEmpty()) {
                hasArmor = true;
                break;
            }
        }

        if (hasArmor) {
            // Remove all armor (shift-click armor slots)
            for (int i = 0; i < 4; i++) {
                if (!mc.player.getEquippedStack(ARMOR_SLOTS[i]).isEmpty()) {
                    // Armor slots in screen handler: 5 (helmet), 6 (chest), 7 (legs), 8 (boots)
                    int screenSlot = 8 - i; // Boots=8, Legs=7, Chest=6, Helmet=5
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        screenSlot, 0,
                        SlotActionType.QUICK_MOVE,
                        mc.player
                    );
                }
            }
        } else {
            // Try to equip armor from inventory
            for (int i = 9; i < 45; i++) {
                var stack = mc.player.currentScreenHandler.getSlot(i).getStack();
                if (!stack.isEmpty() && isArmor(stack.getItem())) {
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        i, 0,
                        SlotActionType.QUICK_MOVE,
                        mc.player
                    );
                }
            }
        }

        // Auto-disable after swapping
        toggle();
    }

    private boolean isArmor(net.minecraft.item.Item item) {
        String name = item.toString().toLowerCase();
        return name.contains("helmet") || name.contains("chestplate") ||
               name.contains("leggings") || name.contains("boots") ||
               name.contains("elytra") || name.contains("turtle");
    }
}
