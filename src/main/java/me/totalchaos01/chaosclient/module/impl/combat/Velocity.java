package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

/**
 * Advanced Velocity module with Cancel and Custom modes.
 * Custom mode scales knockback by horizontal/vertical percentages.
 */
@ModuleInfo(name = "Velocity", description = "Reduces knockback from attacks", category = Category.COMBAT)
public class Velocity extends Module {

    private final NumberSetting horizontal = new NumberSetting("Horizontal", 0, 0, 100, 1);
    private final NumberSetting vertical = new NumberSetting("Vertical", 0, 0, 100, 1);
    private final ModeSetting mode = new ModeSetting("Mode", "Cancel", "Cancel", "Custom", "Jump Reset");

    private boolean wasHurt = false;

    public Velocity() {
        addSettings(mode, horizontal, vertical);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.player == null) return;

        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId()) {
                switch (mode.getMode()) {
                    case "Cancel" -> event.setCancelled(true);
                    case "Custom" -> {
                        // Let the packet through, then modify velocity on next tick
                        wasHurt = true;
                    }
                    case "Jump Reset" -> {
                        event.setCancelled(true);
                        wasHurt = true;
                    }
                }
            }
        }

        if (event.getPacket() instanceof ExplosionS2CPacket) {
            if (mode.is("Cancel")) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        if (!wasHurt) return;
        wasHurt = false;

        switch (mode.getMode()) {
            case "Custom" -> {
                double hPercent = horizontal.getValue() / 100.0;
                double vPercent = vertical.getValue() / 100.0;
                mc.player.setVelocity(
                        mc.player.getVelocity().x * hPercent,
                        mc.player.getVelocity().y * vPercent,
                        mc.player.getVelocity().z * hPercent
                );
            }
            case "Jump Reset" -> {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                }
            }
        }
    }

    @Override
    public String getSuffix() {
        if (mode.is("Custom")) {
            return "H:" + (int) horizontal.getValue() + " V:" + (int) vertical.getValue();
        }
        return mode.getMode();
    }
}

