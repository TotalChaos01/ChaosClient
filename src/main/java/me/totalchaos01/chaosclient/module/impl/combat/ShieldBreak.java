package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

/**
 * ShieldBreak — automatically switches to an axe and attacks
 * when the target is blocking with a shield to disable it.
 * In MC 1.9+, axe attacks disable shields for 5 seconds.
 */
@ModuleInfo(name = "ShieldBreak", description = "Auto-breaks enemy shields with axe", category = Category.COMBAT)
public class ShieldBreak extends Module {

    private final NumberSetting range = new NumberSetting("Range", 3.5, 1.0, 6.0, 0.1);

    private int previousSlot = -1;

    public ShieldBreak() {
        addSettings(range);
    }

    @Override
    protected void onDisable() {
        previousSlot = -1;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Find closest blocking player
        PlayerEntity target = null;
        double closest = range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > closest) continue;

            // Check if target is blocking with shield
            if (player.isBlocking()) {
                target = player;
                closest = dist;
            }
        }

        if (target == null) {
            // Restore previous slot if we switched
            if (previousSlot >= 0) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
            return;
        }

        // Find axe in hotbar
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }

        if (axeSlot >= 0) {
            // Remember current slot and switch to axe
            if (previousSlot < 0) {
                previousSlot = mc.player.getInventory().getSelectedSlot();
            }
            mc.player.getInventory().setSelectedSlot(axeSlot);

            // Attack to disable shield
            if (mc.player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
