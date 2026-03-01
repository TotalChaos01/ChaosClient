package me.totalchaos01.chaosclient.event.events;

import me.totalchaos01.chaosclient.event.Event;
import net.minecraft.network.packet.Packet;

/**
 * Fired when a packet is about to be sent.
 * Can be cancelled to prevent sending.
 */
public class EventPacketSend extends Event {
    private Packet<?> packet;

    public EventPacketSend(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
}

