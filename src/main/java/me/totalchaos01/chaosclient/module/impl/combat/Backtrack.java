package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backtrack — delays incoming entity position updates to extend
 * the effective attack range. Stores recent positions and allows
 * hitting entities at their previous locations.
 *
 * Works by keeping a buffer of past positions and checking if
 * the player can reach any of them within the configured range.
 */
@ModuleInfo(name = "Backtrack", description = "Extends hit range by delaying entity positions", category = Category.COMBAT)
public class Backtrack extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 200, 50, 500, 10);
    private final NumberSetting range = new NumberSetting("Range", 4.0, 2.0, 6.0, 0.1);

    private final Map<UUID, List<TimedPosition>> positionHistory = new ConcurrentHashMap<>();

    public Backtrack() {
        addSettings(delay, range);
    }

    @Override
    protected void onDisable() {
        positionHistory.clear();
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        long maxDelay = (long) delay.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity)) continue;
            if (entity == mc.player) continue;

            UUID uid = entity.getUuid();
            List<TimedPosition> history = positionHistory.computeIfAbsent(uid, k -> new ArrayList<>());

            // Record current position
            history.add(new TimedPosition(new Vec3d(entity.getX(), entity.getY(), entity.getZ()), now));

            // Remove old positions beyond delay window
            history.removeIf(tp -> now - tp.time > maxDelay);
        }

        // Clean up entities that no longer exist
        Set<UUID> activeEntities = new HashSet<>();
        for (Entity e : mc.world.getEntities()) {
            activeEntities.add(e.getUuid());
        }
        positionHistory.keySet().retainAll(activeEntities);
    }

    /**
     * Check if any backtracked position of the given entity is within attack range.
     * Used by combat modules to determine if an entity is hittable.
     */
    public boolean isInBacktrackRange(Entity entity) {
        if (mc.player == null) return false;
        List<TimedPosition> history = positionHistory.get(entity.getUuid());
        if (history == null || history.isEmpty()) return false;

        double maxRange = range.getValue();
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        for (TimedPosition tp : history) {
            if (playerPos.distanceTo(tp.position) <= maxRange) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the closest backtracked position to the player.
     */
    public Vec3d getClosestBacktrackPos(Entity entity) {
        if (mc.player == null) return null;
        List<TimedPosition> history = positionHistory.get(entity.getUuid());
        if (history == null || history.isEmpty()) return null;

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d closest = null;
        double closestDist = Double.MAX_VALUE;

        for (TimedPosition tp : history) {
            double dist = playerPos.distanceTo(tp.position);
            if (dist < closestDist) {
                closestDist = dist;
                closest = tp.position;
            }
        }
        return closest;
    }

    @Override
    public String getSuffix() {
        return String.format("%dms", (int) delay.getValue());
    }

    private record TimedPosition(Vec3d position, long time) {}
}
