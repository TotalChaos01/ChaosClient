package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.command.impl.FriendCommand;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.player.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KillAura v5 — Fixed crits with state machine, enhanced priority, LB-style combat.
 *
 * CRIT FIX: Proper state machine — IDLE → JUMP → WAIT_FALL → ATTACK
 *   Smart: Jumps, waits for descent, then attacks with crit
 *   Force: Always jumps before every attack for guaranteed crits
 *
 * PRIORITY: Angle, Distance, Health, Threat (combined), HurtTime
 */
@ModuleInfo(name = "KillAura", description = "Advanced combat module with Grim/Matrix bypass", category = Category.COMBAT)
public class KillAura extends Module {

    // Targeting
    private final NumberSetting scanRadius = new NumberSetting("Scan Radius", 6.0, 3.0, 10.0, 0.1);
    private final NumberSetting attackRange = new NumberSetting("Attack Range", 3.25, 2.5, 5.0, 0.05);
    private final NumberSetting wallRange = new NumberSetting("Wall Range", 3.0, 0.0, 5.0, 0.05);
    private final ModeSetting priority = new ModeSetting("Priority", "Angle",
            "Angle", "Distance", "Health", "Threat", "HurtTime");
    private final BooleanSetting wallsCheck = new BooleanSetting("Walls Check", false);
    private final BooleanSetting playersOnly = new BooleanSetting("Players Only", false);
    private final BooleanSetting mobsTarget = new BooleanSetting("Target Mobs", true);
    private final BooleanSetting animalsTarget = new BooleanSetting("Target Animals", false);

    // Rotation
    private final ModeSetting rotationMode = new ModeSetting("Rotation Mode", "Smooth", "Smooth", "Snap");
    private final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 42, 15, 60, 1);
    private final BooleanSetting humanize = new BooleanSetting("Humanize", true);
    private final NumberSetting aimTolerance = new NumberSetting("Aim Tolerance", 15.0, 2.0, 25.0, 0.5);
    private final BooleanSetting moveFix = new BooleanSetting("Move Fix", true);

    // Timing
    private final BooleanSetting useCooldown = new BooleanSetting("Use Cooldown", true);
    private final NumberSetting minCps = new NumberSetting("Min CPS", 8, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 12, 1, 20, 1);

    // Combat
    private final BooleanSetting keepSprint = new BooleanSetting("Keep Sprint", true);
    private final ModeSetting critsMode = new ModeSetting("Crits", "Smart", "None", "Smart", "Force");
    private final ModeSetting raycastMode = new ModeSetting("Raycast", "Enemy", "None", "Enemy", "All");

    // Strafe
    private final BooleanSetting targetStrafe = new BooleanSetting("Target Strafe", false);
    private final NumberSetting strafeRadius = new NumberSetting("Strafe Radius", 2.85, 1.5, 5.0, 0.1);

    // Internal state
    private Entity target;
    private final List<Entity> targets = new ArrayList<>();
    private int ticksSinceLastAttack;
    private long nextAttackAt;
    private float currentRangeOffset;
    private boolean preAiming;
    private int noTargetTicks;

    // Crit state machine
    private enum CritState { IDLE, JUMP, WAIT_FALL }
    private CritState critState = CritState.IDLE;
    private int critWaitTicks = 0;

    public Entity getCurrentTarget() { return target; }

    public KillAura() {
        minCps.setVisibility(() -> !useCooldown.isEnabled());
        maxCps.setVisibility(() -> !useCooldown.isEnabled());
        mobsTarget.setVisibility(() -> !playersOnly.isEnabled());
        animalsTarget.setVisibility(() -> !playersOnly.isEnabled());
        rotationSpeed.setVisibility(() -> rotationMode.is("Smooth"));
        humanize.setVisibility(() -> rotationMode.is("Smooth"));

        addSettings(
            scanRadius, attackRange, wallRange, priority, wallsCheck, playersOnly, mobsTarget, animalsTarget,
            rotationMode, rotationSpeed, humanize, aimTolerance, moveFix,
            useCooldown, minCps, maxCps,
            keepSprint, critsMode, raycastMode,
            targetStrafe, strafeRadius
        );
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        ticksSinceLastAttack++;

        // Step 1: Scan for targets
        scanForTargets();

        if (targets.isEmpty()) {
            noTargetTicks++;
            if (target != null) {
                target = null;
                preAiming = false;
                critState = CritState.IDLE;
                critWaitTicks = 0;
                RotationUtil.setMoveCorrection(false);
            }
            if (noTargetTicks > 5) {
                RotationUtil.reset();
            }
            return;
        }
        noTargetTicks = 0;

        // Step 2: Select target with hysteresis
        Entity newTarget = targets.get(0);
        if (target != null && target.isAlive() && newTarget != target) {
            float[] oldRot = RotationUtil.getRotationsToNearest(target);
            float[] newRot = RotationUtil.getRotationsToNearest(newTarget);
            float oldAngle = Math.abs(MathHelper.wrapDegrees(oldRot[0] - RotationUtil.getServerYaw()));
            float newAngle = Math.abs(MathHelper.wrapDegrees(newRot[0] - RotationUtil.getServerYaw()));
            if (newAngle > 55 && oldAngle < 30
                    && mc.player.squaredDistanceTo(target) <= scanRadius.getValue() * scanRadius.getValue()) {
                // Keep old target
            } else {
                target = newTarget;
            }
        } else {
            target = newTarget;
        }

        // Step 3: Rotate
        rotateToTarget(target);

        // Step 3.5: Enable silent movement correction — WASD moves relative to server yaw
        // The actual correction is applied in MixinKeyboardInput AFTER Input.tick()
        // This ensures movementVector isn't overwritten by KeyboardInput.tick()
        RotationUtil.setMoveCorrection(moveFix.isEnabled());

        // Step 4: Range check
        double effectiveRange = attackRange.getValue() + currentRangeOffset;
        boolean canSee = canSeeEntity(target);
        double maxRange = canSee ? effectiveRange : Math.min(effectiveRange, wallRange.getValue());
        boolean inRange = mc.player.squaredDistanceTo(target) <= maxRange * maxRange;

        if (!inRange) {
            preAiming = true;
            return;
        }
        preAiming = false;

        // Step 5: Verify aim
        if (!RotationUtil.isAimingAt(target, (float) aimTolerance.getValue())) {
            return;
        }

        // Step 6: Raycast
        Entity attackTarget = resolveRaycastTarget(target);

        // Step 7: Timing
        if (!canAttackNow()) return;

        // Step 8: Handle crits with state machine
        if (!processCrits()) return;

        // Step 9: Attack — verify entity is still valid
        if (!attackTarget.isAlive() || attackTarget.isRemoved()) return;
        executeAttack(attackTarget);
        critState = CritState.IDLE;
        critWaitTicks = 0;

        // Step 10: Strafe
        if (targetStrafe.isEnabled()) {
            doSafeStrafe(target);
        }
    }

    // ========== CRIT STATE MACHINE ==========

    /**
     * Returns true when we can attack (crit conditions met or not needed).
     * Returns false if we need to wait (jumping/falling).
     */
    private boolean processCrits() {
        if (critsMode.is("None")) return true;

        // If already in crit position, attack immediately
        if (isInCritPosition()) return true;

        if (critsMode.is("Smart")) {
            // Smart: jump and wait for crit position
            if (mc.player.isOnGround() && !mc.player.isTouchingWater() && !mc.player.isInLava()) {
                if (critState == CritState.IDLE) {
                    mc.player.jump();
                    critState = CritState.JUMP;
                    critWaitTicks = 0;
                    return false;
                }
            }
            if (critState == CritState.JUMP || critState == CritState.WAIT_FALL) {
                critWaitTicks++;
                if (isInCritPosition()) return true;
                if (mc.player.isOnGround() && critWaitTicks > 3) {
                    critState = CritState.IDLE;
                    return true; // Timeout: attack without crit
                }
                if (critWaitTicks > 14) {
                    critState = CritState.IDLE;
                    return true; // Safety timeout
                }
                return false;
            }
            return true;
        }

        if (critsMode.is("Force")) {
            // Force: always jump before every attack
            switch (critState) {
                case IDLE -> {
                    if (mc.player.isOnGround() && !mc.player.isTouchingWater() && !mc.player.isInLava()) {
                        mc.player.jump();
                        critState = CritState.JUMP;
                        critWaitTicks = 0;
                    }
                    return false;
                }
                case JUMP -> {
                    critWaitTicks++;
                    if (mc.player.getVelocity().y < 0 && mc.player.fallDistance > 0) {
                        critState = CritState.WAIT_FALL;
                    }
                    if (critWaitTicks > 15) { critState = CritState.IDLE; return true; }
                    return false;
                }
                case WAIT_FALL -> {
                    critWaitTicks++;
                    if (isInCritPosition()) return true;
                    if (mc.player.isOnGround() || critWaitTicks > 20) {
                        critState = CritState.IDLE;
                        return true;
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInCritPosition() {
        return !mc.player.isOnGround()
                && mc.player.fallDistance > 0.0f
                && mc.player.getVelocity().y < 0
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing();
    }

    // ========== RAYCAST ==========

    private Entity resolveRaycastTarget(Entity selectedTarget) {
        if (raycastMode.is("None")) return selectedTarget;
        Entity crosshairEntity = mc.targetedEntity;
        if (crosshairEntity == null || crosshairEntity == selectedTarget) return selectedTarget;
        if (crosshairEntity instanceof LivingEntity le && le.isAlive()) {
            boolean isValid = switch (raycastMode.getMode()) {
                case "Enemy" -> isValidTarget(crosshairEntity);
                case "All" -> crosshairEntity != mc.player;
                default -> false;
            };
            if (isValid) {
                target = crosshairEntity;
                return crosshairEntity;
            }
        }
        return selectedTarget;
    }

    // ========== SCANNING ==========

    private void scanForTargets() {
        targets.clear();
        double scanSq = scanRadius.getValue() * scanRadius.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;
            if (mc.player.squaredDistanceTo(entity) > scanSq) continue;
            if (wallsCheck.isEnabled() && !canSeeEntity(entity)) continue;
            targets.add(entity);
        }

        Comparator<Entity> comp = switch (priority.getMode()) {
            case "Angle" -> Comparator.comparingDouble(e -> {
                float[] rot = RotationUtil.getRotationsToNearest(e);
                float yawDiff = Math.abs(MathHelper.wrapDegrees(rot[0] - RotationUtil.getServerYaw()));
                float pitchDiff = Math.abs(rot[1] - RotationUtil.getServerPitch());
                return yawDiff + pitchDiff;
            });
            case "Health" -> Comparator.comparingDouble(e ->
                (e instanceof LivingEntity le) ? le.getHealth() : Float.MAX_VALUE
            );
            case "Distance" -> Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e));
            case "Threat" -> Comparator.comparingDouble(e -> {
                double dist = mc.player.distanceTo(e);
                float hp = (e instanceof LivingEntity le) ? le.getHealth() : 20f;
                float[] rot = RotationUtil.getRotationsToNearest(e);
                float angle = Math.abs(MathHelper.wrapDegrees(rot[0] - RotationUtil.getServerYaw()));
                return (hp * 0.4) + (dist * 0.3) + (angle * 0.3);
            });
            case "HurtTime" -> Comparator.comparingDouble(e -> {
                int hurtTime = (e instanceof LivingEntity le) ? le.hurtTime : 0;
                double dist = mc.player.squaredDistanceTo(e);
                return hurtTime * 100.0 + dist;
            });
            default -> Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e));
        };
        targets.sort(comp);
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) return false;
        if (!(entity instanceof LivingEntity le)) return false;
        if (le.isDead()) return false;
        if (entity instanceof PlayerEntity p) {
            if (p.isSpectator()) return false;
            if (FriendCommand.isFriend(p.getName().getString())) return false;
            return true;
        }
        if (playersOnly.isEnabled()) return false;
        if (entity instanceof MobEntity) return mobsTarget.isEnabled();
        if (entity instanceof AnimalEntity) return animalsTarget.isEnabled();
        return true;
    }

    private boolean canSeeEntity(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d targetPos = entity.getBoundingBox().getCenter();
        var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
            eye, targetPos, net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
            net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player
        ));
        return hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK ||
               hit.getPos().squaredDistanceTo(eye) >= targetPos.squaredDistanceTo(eye);
    }

    // ========== ROTATION ==========

    private void rotateToTarget(Entity target) {
        float[] rot = RotationUtil.getRotationsToNearest(target);
        if (rotationMode.is("Snap")) {
            // Instant rotation — still apply GCD fix for anti-cheat
            float[] fixed = RotationUtil.applyGCD(rot[0], rot[1]);
            RotationUtil.setRotation(fixed[0], fixed[1], 3);
        } else {
            RotationUtil.smoothRotate(rot[0], rot[1], (float) rotationSpeed.getValue(), humanize.isEnabled());
        }
    }

    // ========== TIMING ==========

    private boolean canAttackNow() {
        if (useCooldown.isEnabled()) {
            return mc.player.getAttackCooldownProgress(0.5f) >= 0.92f;
        } else {
            return System.currentTimeMillis() >= nextAttackAt;
        }
    }

    // ========== ATTACK ==========

    private void executeAttack(Entity target) {
        currentRangeOffset = (float) (ThreadLocalRandom.current().nextDouble(-0.09, 0.12));
        boolean wasSprinting = mc.player.isSprinting();

        mc.getNetworkHandler().sendPacket(
            PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking())
        );
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.player.swingHand(Hand.MAIN_HAND);

        if (keepSprint.isEnabled() && wasSprinting) {
            mc.player.setSprinting(true);
        }

        ticksSinceLastAttack = 0;
        mc.player.resetTicksSinceLastAttack();
        if (!useCooldown.isEnabled()) scheduleNextAttack();
    }

    private void scheduleNextAttack() {
        int cps = ThreadLocalRandom.current().nextInt((int) minCps.getValue(), (int) maxCps.getValue() + 1);
        long baseDelay = 1000L / Math.max(1, cps);
        long jitter = (long) (baseDelay * 0.15 * (Math.random() - 0.5));
        nextAttackAt = System.currentTimeMillis() + baseDelay + jitter;
    }

    // ========== STRAFE ==========

    private void doSafeStrafe(Entity target) {
        double targetX = target.getX();
        double targetZ = target.getZ();
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double angle = Math.atan2(playerZ - targetZ, playerX - targetX);
        double radius = strafeRadius.getValue();
        double speed = 0.22;

        double desiredX = targetX + Math.cos(angle + Math.PI / 4) * radius;
        double desiredZ = targetZ + Math.sin(angle + Math.PI / 4) * radius;

        double moveX = desiredX - playerX;
        double moveZ = desiredZ - playerZ;
        double dist = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (dist > 0.001) {
            moveX = (moveX / dist) * speed;
            moveZ = (moveZ / dist) * speed;
        }

        Vec3d vel = mc.player.getVelocity();
        mc.player.setVelocity(moveX, vel.y, moveZ);
    }

    // ========== LIFECYCLE ==========

    @Override
    protected void onEnable() {
        target = null;
        targets.clear();
        ticksSinceLastAttack = 0;
        nextAttackAt = 0;
        currentRangeOffset = 0;
        preAiming = false;
        noTargetTicks = 0;
        critState = CritState.IDLE;
        critWaitTicks = 0;
    }

    @Override
    protected void onDisable() {
        target = null;
        targets.clear();
        preAiming = false;
        noTargetTicks = 0;
        critState = CritState.IDLE;
        critWaitTicks = 0;
        RotationUtil.setMoveCorrection(false);
        RotationUtil.reset();
    }

    @Override
    public String getSuffix() {
        return target instanceof PlayerEntity p ? p.getName().getString() : "Idle";
    }
}
