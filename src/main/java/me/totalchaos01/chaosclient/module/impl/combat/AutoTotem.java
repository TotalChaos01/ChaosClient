package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.item.Items;

@ModuleInfo(name = "AutoTotem", description = "Automatically places totems in offhand", category = Category.COMBAT)
public class AutoTotem extends Module {

    private final NumberSetting health = new NumberSetting("Health Threshold", 6, 1, 20, 0.5);
    private final BooleanSetting always = new BooleanSetting("Always", false);

    public AutoTotem() {
        addSettings(health, always);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        boolean needsTotem = always.isEnabled() || mc.player.getHealth() <= health.getValue();
        if (!needsTotem) return;

        // Check if totem is already in offhand
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        // Find totem in inventory and swap to offhand
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                // Swap totem to offhand via inventory clicks
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId, i < 9 ? i + 36 : i, 40, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player
                );
                break;
            }
        }
    }
}

