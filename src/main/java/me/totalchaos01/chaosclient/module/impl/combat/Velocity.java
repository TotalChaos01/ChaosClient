package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

@ModuleInfo(name = "Velocity", description = "Reduces knockback from attacks", category = Category.COMBAT)
public class Velocity extends Module {

    private final NumberSetting horizontal = new NumberSetting("Horizontal", 0, 0, 100, 1);
    private final NumberSetting vertical = new NumberSetting("Vertical", 0, 0, 100, 1);
    private final ModeSetting mode = new ModeSetting("Mode", "Cancel", "Cancel", "Custom");

    public Velocity() {
        addSettings(mode, horizontal, vertical);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.player == null) return;

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId()) {
                if (mode.is("Cancel")) {
                    event.setCancelled(true);
                }
            }
        }

        if (event.getPacket() instanceof ExplosionS2CPacket) {
            if (mode.is("Cancel")) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}

