package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.font.ChaosFont;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * LogoutSpots — marks positions where players disconnected with rendering.
 */
@ModuleInfo(name = "LogoutSpots", description = "Shows where players logged out", category = Category.RENDER)
public class LogoutSpots extends Module {

    private final NumberSetting range = new NumberSetting("Range", 256, 64, 512, 16);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", true);
    private final BooleanSetting showTime = new BooleanSetting("Show Time", true);

    private final Map<UUID, LogoutEntry> logoutSpots = new HashMap<>();

    public LogoutSpots() {
        addSettings(range, showDistance, showTime);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.world == null || mc.player == null) return;

        if (event.getPacket() instanceof PlayerRemoveS2CPacket packet) {
            for (UUID uuid : packet.profileIds()) {
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

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        // Remove spots beyond range
        double rangeSq = range.getValue() * range.getValue();
        logoutSpots.entrySet().removeIf(entry -> {
            Vec3d pos = entry.getValue().position;
            return mc.player.squaredDistanceTo(pos) > rangeSq;
        });

        // Remove spots if the player reconnected
        if (mc.world != null) {
            Set<UUID> online = new HashSet<>();
            for (PlayerEntity p : mc.world.getPlayers()) {
                online.add(p.getUuid());
            }
            logoutSpots.entrySet().removeIf(entry -> online.contains(entry.getKey()));
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null || logoutSpots.isEmpty()) return;

        DrawContext ctx = event.getDrawContext();

        for (LogoutEntry entry : logoutSpots.values()) {
            Vec3d pos = entry.position;
            double[] screen = RenderUtil.worldToScreen(pos.x, pos.y + 1.0, pos.z);
            if (screen == null || screen[2] < 0 || screen[2] > 1.0) continue;

            float sx = (float) screen[0];
            float sy = (float) screen[1];

            // Draw marker (red diamond)
            int markerColor = 0xFFFF4444;
            ctx.fill((int) sx - 4, (int) sy, (int) sx, (int) sy - 4, markerColor);
            ctx.fill((int) sx, (int) sy - 4, (int) sx + 4, (int) sy, markerColor);
            ctx.fill((int) sx - 4, (int) sy, (int) sx, (int) sy + 4, markerColor);
            ctx.fill((int) sx, (int) sy + 4, (int) sx + 4, (int) sy, markerColor);

            // Player name
            String name = entry.name;
            int nameWidth = ChaosFont.getWidth(name);
            ChaosFont.drawWithShadow(ctx, name,
                    (int) (sx - nameWidth / 2f), (int) sy - 14, 0xFFFF6666);

            // Distance text
            StringBuilder info = new StringBuilder();
            if (showDistance.isEnabled()) {
                double dist = Math.sqrt(mc.player.squaredDistanceTo(pos));
                info.append(String.format("%.1fm", dist));
            }
            if (showTime.isEnabled()) {
                long elapsed = (System.currentTimeMillis() - entry.time) / 1000;
                if (info.length() > 0) info.append(" | ");
                if (elapsed < 60) info.append(elapsed).append("s ago");
                else info.append(elapsed / 60).append("m ago");
            }

            if (info.length() > 0) {
                String infoStr = info.toString();
                int infoWidth = ChaosFont.getWidth(infoStr);
                ChaosFont.drawWithShadow(ctx, infoStr,
                        (int) (sx - infoWidth / 2f), (int) sy + 6, 0xFFAAAAAA);
            }
        }
    }

    @Override
    protected void onDisable() {
        logoutSpots.clear();
    }

    public record LogoutEntry(String name, Vec3d position, long time) {}
}
