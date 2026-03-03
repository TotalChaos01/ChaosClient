package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

/**
 * NoRotate — prevents the server from forcefully changing your head rotation.
 * Intercepts PlayerPositionLookS2CPacket and replaces yaw/pitch with current values.
 */
@ModuleInfo(name = "NoRotate", description = "Prevents server from changing your rotation", category = Category.PLAYER)
public class NoRotate extends Module {

    private final BooleanSetting noYaw = new BooleanSetting("No Yaw", true);
    private final BooleanSetting noPitch = new BooleanSetting("No Pitch", true);

    public NoRotate() {
        addSettings(noYaw, noPitch);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.player == null) return;

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            // We can't modify the S2C packet fields directly without accessor,
            // so we just restore the player's rotation after the packet is processed.
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();

            // Schedule rotation restore for next tick
            mc.execute(() -> {
                if (mc.player == null) return;
                if (noYaw.isEnabled()) mc.player.setYaw(yaw);
                if (noPitch.isEnabled()) mc.player.setPitch(pitch);
            });
        }
    }
}
