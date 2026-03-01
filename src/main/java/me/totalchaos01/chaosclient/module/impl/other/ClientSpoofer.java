package me.totalchaos01.chaosclient.module.impl.other;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;

/**
 * Spoofs the client brand sent to the server.
 */
@ModuleInfo(name = "ClientSpoofer", description = "Spoofs client brand", category = Category.OTHER)
public class ClientSpoofer extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Lunar", "Badlion", "Forge", "Optifine");

    public ClientSpoofer() {
        addSettings(mode);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        // Intercept ClientCustomPayloadC2SPacket to change brand
        // Implementation depends on exact packet structure in 1.21.1
    }
}
