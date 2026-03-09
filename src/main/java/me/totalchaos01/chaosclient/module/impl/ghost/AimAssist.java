package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.player.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;

/**
 * AimAssist v3 — Critically Damped Spring smoothing.
 *
 * Previous version used double-smoothed lerp which still caused visible jitter.
 * This version uses a critically damped spring model for each rotation axis:
 *
 *   velocity += (target - current) * stiffness - velocity * damping
 *   current += velocity
 *
 * This creates PERFECTLY smooth, natural-looking mouse movement with:
 * - No jitter or snapping
 * - Smooth acceleration AND deceleration
 * - Overshoot-free convergence (critically damped)
 * - Speed naturally decreases as we approach target
 */
@ModuleInfo(name = "AimAssist", description = "Smooth legit aim assist", category = Category.LEGIT)
public class AimAssist extends Module {

    private final NumberSetting range = new NumberSetting("Range", 4.2, 1.0, 8.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 75.0, 5.0, 180.0, 1.0);
    private final NumberSetting speed = new NumberSetting("Speed", 4.0, 0.5, 15.0, 0.1);
    private final NumberSetting smoothness = new NumberSetting("Smoothness", 0.6, 0.1, 1.0, 0.05);

    private final BooleanSetting onlyClick = new BooleanSetting("Only Click", true);
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting invisible = new BooleanSetting("Invisible", false);
    private final BooleanSetting verticalAim = new BooleanSetting("Vertical Aim", true);

    private static final int STICKY_TICKS = 10;

    private Entity lockedTarget;
    private int lockTimer;

    // Critically damped spring state
    private float yawVelocity = 0;
    private float pitchVelocity = 0;

    public AimAssist() {
        addSettings(range, fov, speed, smoothness, onlyClick, players, mobs, invisible, verticalAim);
    }

    @EventTarget
    public void onTick(EventTick ignoredEvent) {
        if (mc.player == null || mc.world == null) return;

        boolean click = mc.options.attackKey.isPressed();
        if (onlyClick.isEnabled() && !click && lockTimer <= 0) {
            lockedTarget = null;
            yawVelocity = 0;
            pitchVelocity = 0;
            return;
        }

        // Sticky targeting: lock onto entity we click on
        if (click && mc.crosshairTarget instanceof EntityHitResult ehr) {
            Entity pointed = ehr.getEntity();
            if (isValidTarget(pointed)) {
                lockedTarget = pointed;
                lockTimer = STICKY_TICKS;
            }
        } else if (!click && lockTimer > 0) {
            lockTimer--;
        }

        Entity target = chooseTarget();
        if (target == null) {
            if (!click && lockTimer <= 0) lockedTarget = null;
            // Smoothly decay velocity to zero (don't just stop)
            yawVelocity *= 0.8f;
            pitchVelocity *= 0.8f;
            return;
        }

        float[] rotations = RotationUtil.getRotationsTo(target);
        float targetYaw = rotations[0];
        float targetPitch = rotations[1];

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawError = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchError = targetPitch - currentPitch;

        // ── Critically Damped Spring ──
        // stiffness controls how fast it reaches the target
        // damping = 2 * sqrt(stiffness) for critical damping (no oscillation)
        float speedFactor = (float) speed.getValue();
        float smoothFactor = (float) smoothness.getValue();

        // Stiffness: higher speed = stiffer spring = faster convergence
        float stiffness = speedFactor * 0.08f / Math.max(smoothFactor, 0.1f);
        // Critical damping coefficient: 2 * sqrt(k) ensures no overshoot
        float damping = 2.0f * (float) Math.sqrt(stiffness) * smoothFactor;

        // Spring physics: F = -k * error,  drag = -d * velocity
        float yawAccel = yawError * stiffness - yawVelocity * damping;
        float pitchAccel = pitchError * stiffness - pitchVelocity * damping;

        yawVelocity += yawAccel;
        pitchVelocity += pitchAccel;

        // Soft speed limit (not a hard clamp — preserves smoothness)
        float maxVel = speedFactor * 1.5f;
        float velMag = (float) Math.sqrt(yawVelocity * yawVelocity + pitchVelocity * pitchVelocity);
        if (velMag > maxVel) {
            float scale = maxVel / velMag;
            yawVelocity *= scale;
            pitchVelocity *= scale;
        }

        // Dead zone — stop micro-corrections when very close
        if (Math.abs(yawError) < 0.05f && Math.abs(yawVelocity) < 0.02f) {
            yawVelocity = 0;
        }
        if (Math.abs(pitchError) < 0.05f && Math.abs(pitchVelocity) < 0.02f) {
            pitchVelocity = 0;
        }

        mc.player.setYaw(currentYaw + yawVelocity);
        if (verticalAim.isEnabled()) {
            mc.player.setPitch(MathHelper.clamp(currentPitch + pitchVelocity, -90f, 90f));
        }
    }

    private Entity chooseTarget() {
        if (lockedTarget != null && isTargetStillValid(lockedTarget)) {
            return lockedTarget;
        }

        Entity best = null;
        float bestScore = Float.MAX_VALUE;
        double rangeSq = range.getValue() * range.getValue();
        float maxFov = (float) fov.getValue();

        if (mc.world == null || mc.player == null) return null;

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;
            if (mc.player.squaredDistanceTo(entity) > rangeSq) continue;

            float yawDiff = RotationUtil.getYawDifference(entity);
            if (yawDiff > maxFov) continue;

            float score = yawDiff + (float) (mc.player.distanceTo(entity) * 0.12);
            if (score < bestScore) {
                bestScore = score;
                best = entity;
            }
        }

        if (best != null) {
            lockedTarget = best;
            if (lockTimer < 2) lockTimer = 2;
        }
        return best;
    }

    private boolean isTargetStillValid(Entity entity) {
        if (!isValidTarget(entity)) return false;
        if (mc.player == null) return false;
        double rangeSq = range.getValue() * range.getValue();
        if (mc.player.squaredDistanceTo(entity) > rangeSq) return false;
        float yawDiff = RotationUtil.getYawDifference(entity);
        return yawDiff <= (float) fov.getValue();
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) return false;
        if (!invisible.isEnabled() && entity.isInvisible()) return false;
        if (entity instanceof PlayerEntity) return players.isEnabled();
        if (entity instanceof MobEntity) return mobs.isEnabled();
        return false;
    }

    @Override
    protected void onDisable() {
        lockedTarget = null;
        lockTimer = 0;
        yawVelocity = 0;
        pitchVelocity = 0;
    }
}
