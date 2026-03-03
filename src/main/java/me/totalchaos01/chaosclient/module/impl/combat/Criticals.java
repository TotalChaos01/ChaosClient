package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * Automatically lands critical hits by sending position packets
 * that simulate being airborne (falling) at the moment of attack.
 */
@ModuleInfo(name = "Criticals", description = "Automatically land critical hits", category = Category.COMBAT)
public class Criticals extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Jump", "MiniJump");

    public Criticals() {
        addSettings(mode);
    }

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isOnGround()) return;

        // Detect attack packet
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            switch (mode.getMode()) {
                case "Packet" -> {
                    // Send fake position packets to simulate falling
                    // Server thinks player is airborne → critical hit
                    double x = mc.player.getX();
                    double y = mc.player.getY();
                    double z = mc.player.getZ();

                    // Tiny upward movement + fall sequence
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, mc.player.horizontalCollision));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, mc.player.horizontalCollision));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.1E-5, z, false, mc.player.horizontalCollision));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, mc.player.horizontalCollision));
                }
                case "Jump" -> {
                    // Full jump for criticals
                    mc.player.jump();
                }
                case "MiniJump" -> {
                    // Small hop — set upward velocity directly
                    mc.player.setVelocity(mc.player.getVelocity().add(0, 0.1, 0));
                }
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}

