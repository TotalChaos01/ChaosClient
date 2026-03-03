package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;

/**
 * AutoLog — automatically disconnects when your health drops below a threshold.
 */
@ModuleInfo(name = "AutoLog", description = "Auto-disconnects on low health", category = Category.PLAYER)
public class AutoLog extends Module {

    private final NumberSetting health = new NumberSetting("Health", 6, 1, 20, 1);
    private final BooleanSetting onlyInPvP = new BooleanSetting("Only PvP", false);

    public AutoLog() {
        addSettings(health, onlyInPvP);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.getHealth() <= health.getValue() && mc.player.getHealth() > 0) {
            if (onlyInPvP.isEnabled()) {
                boolean playerNearby = false;
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player == mc.player) continue;
                    if (mc.player.distanceTo(player) < 16) {
                        playerNearby = true;
                        break;
                    }
                }
                if (!playerNearby) return;
            }

            mc.player.networkHandler.getConnection().disconnect(
                net.minecraft.text.Text.literal("§c[ChaosClient] AutoLog: Health below " + (int) health.getValue())
            );
            toggle();
        }
    }

    @Override
    public String getSuffix() {
        return String.valueOf((int) health.getValue());
    }
}
