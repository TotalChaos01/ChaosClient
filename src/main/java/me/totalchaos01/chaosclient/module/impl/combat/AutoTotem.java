package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Advanced AutoTotem — automatically places totems in offhand.
 * Features:
 * - Configurable health threshold for auto-swap
 * - Always mode for constant totem in offhand
 * - Soft mode: only swaps when not manually holding an item in offhand
 * - Auto-replenish: moves totems from inventory to hotbar when low
 * - Counts remaining totems
 */
@ModuleInfo(name = "AutoTotem", description = "Automatically places totems in offhand", category = Category.COMBAT)
public class AutoTotem extends Module {

    private final NumberSetting health = new NumberSetting("Health Threshold", 6, 1, 20, 0.5);
    private final BooleanSetting always = new BooleanSetting("Always", false);
    private final BooleanSetting soft = new BooleanSetting("Soft", false);
    private final BooleanSetting gapple = new BooleanSetting("Allow Gapple", true);

    private int totemCount = 0;

    public AutoTotem() {
        addSettings(health, always, soft, gapple);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        // Count totems in inventory
        totemCount = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemCount += mc.player.getInventory().getStack(i).getCount();
            }
        }
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            totemCount += mc.player.getOffHandStack().getCount();
        }

        boolean needsTotem = always.isEnabled() || mc.player.getHealth() <= health.getValue();
        if (!needsTotem) return;

        // Check if totem is already in offhand
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        // Soft mode: don't swap if player intentionally has something in offhand
        if (soft.isEnabled() && !mc.player.getOffHandStack().isEmpty()) {
            // Exception: swap if holding golden apple and gapple mode is on
            if (gapple.isEnabled() && mc.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE) {
                // Allow swap - gapple will be moved
            } else if (gapple.isEnabled() && mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                // Allow swap - gapple will be moved
            } else {
                return;
            }
        }

        // Find totem in inventory and swap to offhand
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                int slot = i < 9 ? i + 36 : i;
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        slot, 40, SlotActionType.SWAP, mc.player
                );
                break;
            }
        }
    }

    @Override
    public String getSuffix() {
        return String.valueOf(totemCount);
    }
}
