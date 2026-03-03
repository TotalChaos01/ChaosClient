package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * LogoutSpots — marks positions where other players disconnected.
 */
@ModuleInfo(name = "LogoutSpots", description = "Shows where players logged out", category = Category.RENDER)
public class LogoutSpots extends Module {

    private final NumberSetting range = new NumberSetting("Range", 256, 64, 512, 16);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", true);

    private final Map<UUID, LogoutEntry> logoutSpots = new HashMap<>();
    private final Set<UUID> trackedPlayers = new HashSet<>();

    public LogoutSpots() {
        addSettings(range, showDistance);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.world == null || mc.player == null) return;

        // Track all current players
        Set<UUID> currentPlayers = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            currentPlayers.add(player.getUuid());

            // If player was not tracked before, they just joined
            if (!trackedPlayers.contains(player.getUuid())) {
                // Remove their old logout spot if they reconnected
                logoutSpots.remove(player.getUuid());
            }
        }

        // Detect disconnected players
        for (UUID tracked : trackedPlayers) {
            if (!currentPlayers.contains(tracked)) {
                // Player disappeared from the world — they logged out
                // Find their last known position from the world
            }
        }

        trackedPlayers.clear();
        trackedPlayers.addAll(currentPlayers);

        // Remove spots that are too far away
        logoutSpots.entrySet().removeIf(entry -> {
            Vec3d pos = entry.getValue().position;
            return mc.player.squaredDistanceTo(pos) > range.getValue() * range.getValue();
        });
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.world == null || mc.player == null) return;

        if (event.getPacket() instanceof PlayerRemoveS2CPacket packet) {
            for (UUID uuid : packet.profileIds()) {
                // Find the player entity before they are removed
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (player.getUuid().equals(uuid) && player != mc.player) {
                        logoutSpots.put(uuid, new LogoutEntry(
                            player.getName().getString(),
                            new Vec3d(player.getX(), player.getY(), player.getZ()),
                            System.currentTimeMillis()
                        ));
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        logoutSpots.clear();
        trackedPlayers.clear();
    }

    public Map<UUID, LogoutEntry> getLogoutSpots() {
        return logoutSpots;
    }

    public record LogoutEntry(String name, Vec3d position, long time) {}
}
