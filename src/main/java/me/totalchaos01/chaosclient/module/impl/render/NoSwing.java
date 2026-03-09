package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;

/**
 * NoSwing module — ported from LiquidBounce
 *
 * Disables the arm swing effect.
 * Can hide swing on client side (visual), server side (packet), or both.
 */
@ModuleInfo(name = "NoSwing", description = "Disables arm swing animation", category = Category.RENDER)
public class NoSwing extends Module {

    private final BooleanSetting hideClient = new BooleanSetting("Hide Client", true);
    private final BooleanSetting hideServer = new BooleanSetting("Hide Server", false);

    public NoSwing() {
        addSettings(hideClient, hideServer);
    }

    /**
     * Cancel the swing packet (server-side) if configured.
     */
    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (hideServer.isEnabled() && event.getPacket() instanceof HandSwingC2SPacket) {
            event.setCancelled(true);
        }
    }

    /**
     * Used by mixin/rendering to check if client-side swing animation should be hidden.
     */
    public boolean shouldHideClientSwing() {
        return isEnabled() && hideClient.isEnabled();
    }
}
