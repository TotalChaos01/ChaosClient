package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * AutoArmor — automatically equips the best armor from your inventory.
 */
@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor", category = Category.PLAYER)
public class AutoArmor extends Module {

    private final NumberSetting delay = new NumberSetting("Delay (ticks)", 2, 0, 10, 1);
    private final BooleanSetting preferElytra = new BooleanSetting("Prefer Elytra", false);

    private int tickTimer = 0;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    public AutoArmor() {
        addSettings(delay, preferElytra);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        tickTimer++;
        if (tickTimer < delay.getValue()) return;
        tickTimer = 0;

        // Check each armor slot
        for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
            if (mc.player.getEquippedStack(ARMOR_SLOTS[armorSlot]).isEmpty()) {
                // Find best armor for this slot in inventory
                int bestSlot = findBestArmorForSlot(armorSlot);
                if (bestSlot != -1) {
                    equipArmor(bestSlot, armorSlot);
                    return; // One action per tick
                }
            }
        }
    }

    private int findBestArmorForSlot(int armorSlot) {
        int bestSlot = -1;
        int bestProtection = -1;

        for (int i = 0; i < 36; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            // Check if this is an elytra for chestplate slot
            if (armorSlot == 2 && preferElytra.isEnabled() && stack.getItem() == Items.ELYTRA) {
                return i;
            }

            int slot = getArmorSlotForItem(stack.getItem());
            if (slot != armorSlot) continue;

            int protection = getProtectionValue(stack.getItem());
            if (protection > bestProtection) {
                bestProtection = protection;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int getArmorSlotForItem(net.minecraft.item.Item item) {
        if (item == Items.LEATHER_HELMET || item == Items.CHAINMAIL_HELMET ||
            item == Items.IRON_HELMET || item == Items.GOLDEN_HELMET ||
            item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET ||
            item == Items.TURTLE_HELMET) return 3;
        if (item == Items.LEATHER_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE ||
            item == Items.IRON_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE ||
            item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE ||
            item == Items.ELYTRA) return 2;
        if (item == Items.LEATHER_LEGGINGS || item == Items.CHAINMAIL_LEGGINGS ||
            item == Items.IRON_LEGGINGS || item == Items.GOLDEN_LEGGINGS ||
            item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) return 1;
        if (item == Items.LEATHER_BOOTS || item == Items.CHAINMAIL_BOOTS ||
            item == Items.IRON_BOOTS || item == Items.GOLDEN_BOOTS ||
            item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) return 0;
        return -1;
    }

    private int getProtectionValue(net.minecraft.item.Item item) {
        if (item.toString().contains("netherite")) return 5;
        if (item.toString().contains("diamond")) return 4;
        if (item.toString().contains("iron")) return 3;
        if (item.toString().contains("chainmail")) return 2;
        if (item.toString().contains("golden")) return 2;
        if (item.toString().contains("leather")) return 1;
        if (item == Items.TURTLE_HELMET) return 3;
        if (item == Items.ELYTRA) return 0;
        return 0;
    }

    private void equipArmor(int fromSlot, int armorSlot) {
        // Convert inventory slot index to screen handler slot
        int screenSlot;
        if (fromSlot < 9) {
            screenSlot = fromSlot + 36; // Hotbar: 0-8 → 36-44
        } else {
            screenSlot = fromSlot; // Main inventory: 9-35
        }

        // Shift-click to equip
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            screenSlot,
            0,
            SlotActionType.QUICK_MOVE,
            mc.player
        );
    }
}
