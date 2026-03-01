package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Blink", description = "Delays sending packets to server", category = Category.PLAYER)
public class Blink extends Module {

    private final List<Packet<?>> packetQueue = new ArrayList<>();

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (mc.player == null) return;
        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            packetQueue.add(event.getPacket());
            event.setCancelled(true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null && mc.player.networkHandler != null) {
            for (Packet<?> packet : packetQueue) {
                mc.player.networkHandler.sendPacket(packet);
            }
        }
        packetQueue.clear();
    }

    @Override
    protected void onEnable() {
        packetQueue.clear();
    }
}

