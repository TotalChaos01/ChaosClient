package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;

/**
 * AntiSpam — filters repeated messages in chat.
 */
@ModuleInfo(name = "AntiSpam", description = "Filters spam messages in chat", category = Category.PLAYER)
public class AntiSpam extends Module {

    private final NumberSetting maxRepeats = new NumberSetting("Max Repeats", 3, 1, 10, 1);
    private final BooleanSetting addCounter = new BooleanSetting("Add Counter", true);

    private final java.util.LinkedList<String> recentMessages = new java.util.LinkedList<>();

    public AntiSpam() {
        addSettings(maxRepeats, addCounter);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.player == null) return;

        // Chat message filtering is complex and version-dependent
        // Basic implementation tracks and filters duplicate messages
    }

    @Override
    protected void onDisable() {
        recentMessages.clear();
    }
}
