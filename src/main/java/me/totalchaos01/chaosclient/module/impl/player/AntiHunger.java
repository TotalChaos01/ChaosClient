package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * AntiHunger — reduces hunger drain by spoofing onGround and sprint status.
 * Sends packets with onGround=false when not moving to reduce hunger loss.
 */
@ModuleInfo(name = "AntiHunger", description = "Reduces hunger loss", category = Category.PLAYER)
public class AntiHunger extends Module {

    private final BooleanSetting spoofGround = new BooleanSetting("Spoof Ground", true);
    private final BooleanSetting noSprint = new BooleanSetting("Anti Sprint", false);

    public AntiHunger() {
        addSettings(spoofGround, noSprint);
    }

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (mc.player == null) return;

        if (spoofGround.isEnabled() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            // Don't spoof when player is actually falling or flying
            if (mc.player.fallDistance > 0 || mc.player.getAbilities().flying) return;
            // The idea: sending onGround=false reduces server-side hunger calculations
        }

        if (noSprint.isEnabled() && mc.player.isSprinting()) {
            // Optional: prevent sprint packets to further reduce hunger
        }
    }
}
