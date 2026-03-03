package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * AutoGapple — automatically eats golden apples when health is low.
 */
@ModuleInfo(name = "AutoGapple", description = "Automatically eats golden apples", category = Category.COMBAT)
public class AutoGapple extends Module {

    private final NumberSetting healthThreshold = new NumberSetting("Health", 10, 1, 20, 1);

    private boolean eating = false;
    private int previousSlot = -1;

    public AutoGapple() {
        addSettings(healthThreshold);
    }

    @Override
    protected void onDisable() {
        if (eating && mc.player != null) {
            mc.options.useKey.setPressed(false);
            if (previousSlot != -1) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
            eating = false;
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.interactionManager == null) return;

        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (health <= healthThreshold.getValue()) {
            if (!eating) {
                // Find golden apple
                int slot = findGoldenApple();
                if (slot == -1) return;

                previousSlot = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(slot);
                eating = true;
            }
            // Hold right click to eat
            mc.options.useKey.setPressed(true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } else if (eating) {
            // Health recovered, stop eating
            mc.options.useKey.setPressed(false);
            if (previousSlot != -1) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
            eating = false;
        }
    }

    private int findGoldenApple() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                return i;
            }
        }
        return -1;
    }
}
