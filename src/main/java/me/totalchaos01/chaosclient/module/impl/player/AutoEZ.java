package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

import java.util.Random;

/**
 * AutoEZ — sends a message in chat after killing a player.
 */
@ModuleInfo(name = "AutoEZ", description = "Sends a message after a kill", category = Category.PLAYER)
public class AutoEZ extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Custom", "Custom", "GG", "EZ", "Random");
    private final NumberSetting cooldown = new NumberSetting("Cooldown (s)", 5, 1, 30, 1);

    private long lastMessageTime = 0;
    private final String[] randomMessages = {
        "GG!", "EZ", "Too easy", "Get good", "Rekt", "Nice try", "L"
    };
    private final Random random = new Random();

    public AutoEZ() {
        addSettings(mode, cooldown);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (now - lastMessageTime < cooldown.getValue() * 1000) return;

        // Detection: check if a nearby player died recently
        // This is a simplified approach
    }

    /**
     * Called when a kill is detected (from external event).
     */
    public void onKill(String victimName) {
        long now = System.currentTimeMillis();
        if (now - lastMessageTime < cooldown.getValue() * 1000) return;

        String message;
        switch (mode.getMode()) {
            case "GG" -> message = "GG " + victimName;
            case "EZ" -> message = "EZ " + victimName;
            case "Random" -> message = randomMessages[random.nextInt(randomMessages.length)];
            default -> message = "ChaosClient on top!";
        }

        if (mc.player != null) {
            mc.player.networkHandler.sendChatMessage(message);
            lastMessageTime = now;
        }
    }
}
