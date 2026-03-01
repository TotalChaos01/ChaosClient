package me.totalchaos01.chaosclient.event.events;

import me.totalchaos01.chaosclient.event.Event;
import net.minecraft.network.packet.Packet;

/**
 * Fired when a packet is received from the server.
 * Can be cancelled to ignore the packet.
 */
public class EventPacketReceive extends Event {
    private Packet<?> packet;

    public EventPacketReceive(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}

