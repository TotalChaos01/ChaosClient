package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.screen.GenericContainerScreenHandler;

@ModuleInfo(name = "ChestStealer", description = "Automatically steals items from chests", category = Category.PLAYER)
public class ChestStealer extends Module {

    private final NumberSetting delay = new NumberSetting("Delay (ms)", 50, 0, 500, 10);

    private long lastSteal;

    public ChestStealer() {
        addSettings(delay);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container)) return;

        if (System.currentTimeMillis() - lastSteal < delay.getValue()) return;

        int containerSize = container.getRows() * 9;
        boolean hasItems = false;

        for (int i = 0; i < containerSize; i++) {
            if (!container.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                        container.syncId, i, 0,
                        net.minecraft.screen.slot.SlotActionType.QUICK_MOVE, mc.player
                );
                lastSteal = System.currentTimeMillis();
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            mc.player.closeHandledScreen();
        }
    }
}

