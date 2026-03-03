package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * AutoMending — automatically holds mending items in hand to repair them with XP.
 * Switches damaged mending items to main hand when near XP orbs.
 */
@ModuleInfo(name = "AutoMending", description = "Auto repair items with XP", category = Category.COMBAT)
public class AutoMending extends Module {

    private final NumberSetting durabilityThreshold = new NumberSetting("Min Durability %", 80, 10, 100, 5);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);

    public AutoMending() {
        addSettings(durabilityThreshold, autoSwitch);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        // Check if there are nearby XP orbs
        boolean xpNearby = mc.world.getEntities().spliterator().estimateSize() > 0 && hasXPNearby();

        if (!xpNearby) return;

        // Find the most damaged mending item in armor/offhand
        if (autoSwitch.isEnabled()) {
            int damagedSlot = findMostDamagedMendingItem();
            if (damagedSlot != -1) {
                // Move to main hand
                mc.player.getInventory().setSelectedSlot(damagedSlot);
            }
        }
    }

    private boolean hasXPNearby() {
        for (var entity : mc.world.getEntities()) {
            if (entity instanceof net.minecraft.entity.ExperienceOrbEntity) {
                if (mc.player.distanceTo(entity) < 3) return true;
            }
        }
        return false;
    }

    private int findMostDamagedMendingItem() {
        int worstSlot = -1;
        float worstDurability = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !stack.isDamageable()) continue;

            float durability = 1.0f - (float) stack.getDamage() / stack.getMaxDamage();
            if (durability < durabilityThreshold.getValue() / 100.0 && durability < worstDurability) {
                worstDurability = durability;
                worstSlot = i;
            }
        }

        return worstSlot;
    }
}
