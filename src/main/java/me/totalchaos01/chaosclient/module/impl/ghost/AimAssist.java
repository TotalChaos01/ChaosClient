package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Subtle aim correction towards nearby players.
 * Features: smooth rotation, randomization for legit look,
 * break blocks check, only when attack key held option.
 */
@ModuleInfo(name = "AimAssist", description = "Subtly aims at nearby players", category = Category.LEGIT)
public class AimAssist extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", 30, 5, 100, 1);
    private final NumberSetting fov = new NumberSetting("FOV", 90, 10, 360, 5);
    private final NumberSetting range = new NumberSetting("Range", 4.5, 1, 8, 0.5);
    private final BooleanSetting onlyOnClick = new BooleanSetting("Only On Click", false);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", true);

    public AimAssist() {
        addSettings(speed, fov, range, onlyOnClick, breakBlocks, randomize);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        // Only aim when clicking if setting enabled
        if (onlyOnClick.isEnabled() && !mc.options.attackKey.isPressed()) return;

        // Don't interfere when breaking blocks
        if (!breakBlocks.isEnabled() && mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double diffY = (target.getY() + target.getEyeHeight(target.getPose())) -
                (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) -(Math.toDegrees(Math.atan2(diffY, dist)));

        float yawDiff = wrapAngle(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();

        float aimSpeed = (float) (speed.getValue() / 100.0);

        // Add small randomization for legit appearance
        if (randomize.isEnabled()) {
            float rand = (float) (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4f;
            yawDiff += rand;
            pitchDiff += (float) (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2f;
        }

        mc.player.setYaw(mc.player.getYaw() + yawDiff * aimSpeed);
        mc.player.setPitch(Math.max(-90, Math.min(90, mc.player.getPitch() + pitchDiff * aimSpeed)));
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDist = range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > bestDist) continue;

            // FOV check
            double angle = getAngleTo(player);
            if (angle > fov.getValue() / 2.0) continue;

            bestDist = dist;
            best = player;
        }
        return best;
    }

    private double getAngleTo(Entity entity) {
        double diffX = entity.getX() - mc.player.getX();
        double diffZ = entity.getZ() - mc.player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float diff = Math.abs(wrapAngle(mc.player.getYaw() - yaw));
        return diff;
    }

    private float wrapAngle(float angle) {
        angle %= 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }
}

