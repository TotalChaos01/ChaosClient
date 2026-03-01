package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;

@ModuleInfo(name = "NoFall", description = "Prevents fall damage", category = Category.PLAYER)
public class NoFall extends Module {

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (mc.player == null) return;

        // NoFall sets onGround to true in movement packets
        // This is handled via mixin to modify the packet before sending
        if (mc.player.fallDistance > 2.5f) {
            mc.player.networkHandler.sendPacket(
                    new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly(true, false)
            );
        }
    }
}

