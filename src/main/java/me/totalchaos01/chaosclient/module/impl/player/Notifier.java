package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

/**
 * Notifier — sends notifications for various in-game events.
 * Detects totem pops, player joins/leaves, and other notable events.
 */
@ModuleInfo(name = "Notifier", description = "Notifies about in-game events", category = Category.PLAYER)
public class Notifier extends Module {

    private final BooleanSetting totemPops = new BooleanSetting("Totem Pops", true);
    private final BooleanSetting visualRange = new BooleanSetting("Visual Range", true);

    private final java.util.Map<String, Integer> popCounts = new java.util.HashMap<>();
    private final java.util.Set<String> trackedPlayers = new java.util.HashSet<>();

    public Notifier() {
        addSettings(totemPops, visualRange);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.world == null || mc.player == null) return;

        if (totemPops.isEnabled() && event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 35) { // Totem pop status
                var entity = packet.getEntity(mc.world);
                if (entity instanceof PlayerEntity player && player != mc.player) {
                    String name = player.getName().getString();
                    int pops = popCounts.getOrDefault(name, 0) + 1;
                    popCounts.put(name, pops);

                    sendChatNotification("§6[Notifier] §f" + name + " §epopped a totem! §f(x" + pops + ")");
                }
            }
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.world == null || mc.player == null) return;

        // Visual range tracking
        if (visualRange.isEnabled()) {
            java.util.Set<String> currentPlayers = new java.util.HashSet<>();
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                String name = player.getName().getString();
                currentPlayers.add(name);

                if (!trackedPlayers.contains(name)) {
                    sendChatNotification("§6[Notifier] §a" + name + " §eentered visual range");
                }
            }

            for (String name : trackedPlayers) {
                if (!currentPlayers.contains(name)) {
                    sendChatNotification("§6[Notifier] §c" + name + " §eleft visual range");
                    popCounts.remove(name);
                }
            }

            trackedPlayers.clear();
            trackedPlayers.addAll(currentPlayers);
        }

        // Clean pop counts for dead/gone players
        popCounts.entrySet().removeIf(entry -> {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p.getName().getString().equals(entry.getKey())) {
                    if (p.isDead()) return true;
                    return false;
                }
            }
            return true;
        });
    }

    private void sendChatNotification(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }

    @Override
    protected void onDisable() {
        popCounts.clear();
        trackedPlayers.clear();
    }
}
