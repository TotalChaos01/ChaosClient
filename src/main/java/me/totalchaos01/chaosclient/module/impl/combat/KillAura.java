package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KillAura — auto attack with AC-safe rotation, smart crits,
 * range ring visualization, scan range for pre-aim.
 */
@ModuleInfo(name = "KillAura", description = "Automatically attacks nearby entities", category = Category.COMBAT)
public class KillAura extends Module {

    // Main
    private final ModeSetting attackMode = new ModeSetting("Mode", "1.9+", "1.9+", "1.8");
    private final NumberSetting range = new NumberSetting("Range", 3.5, 1.0, 6.0, 0.1);
    private final NumberSetting scanRange = new NumberSetting("Scan Range", 2.0, 0.0, 4.0, 0.1);
    private final NumberSetting minCps = new NumberSetting("Min CPS", 10, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 14, 1, 20, 1);
    private final NumberSetting minCooldown = new NumberSetting("Min Cooldown", 0.93, 0.70, 1.0, 0.01);

    // Targeting
    private final ModeSetting priority = new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);

    // Crits — always critical without forced jumping
    private final ModeSetting critsMode = new ModeSetting("Crits", "None", "None", "Packet", "Smart");
    private final BooleanSetting rangeRing = new BooleanSetting("Range Ring", true);

    // Rotation — GCD-fixed server-side for AC bypass
    private final ModeSetting rotMode = new ModeSetting("Rotation", "Server", "Client", "Server");
    private final NumberSetting rotSpeed = new NumberSetting("Rot Speed", 0.8, 0.1, 1.0, 0.05);

    // State
    private long lastAttackTime;
    private long currentDelay = 80;
    private LivingEntity currentTarget;
    private LivingEntity scanTarget;
    private float serverYaw, serverPitch;
    private boolean hasServerRotation = false;

    // GCD constant for Minecraft (sensitivity-based angle snapping)
    private static final float GCD = 0.005493164f;

    public KillAura() {
        minCps.setVisibility(() -> attackMode.is("1.8"));
        maxCps.setVisibility(() -> attackMode.is("1.8"));
        minCooldown.setVisibility(() -> attackMode.is("1.9+"));

        addSettings(attackMode, range, scanRange, minCps, maxCps, minCooldown, priority,
                players, mobs, animals, critsMode, rangeRing, rotMode, rotSpeed);
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        scanTarget = null;
        hasServerRotation = false;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        double attackRange = range.getValue();
        double totalRange = attackRange + scanRange.getValue();

        // Find targets
        currentTarget = findTarget(attackRange);
        scanTarget = currentTarget != null ? currentTarget : findTarget(totalRange);

        if (currentTarget == null && scanTarget == null) {
            hasServerRotation = false;
            return;
        }

        // Rotate to target (scan or attack)
        LivingEntity rotTarget = currentTarget != null ? currentTarget : scanTarget;
        if (rotTarget != null) {
            if (rotMode.is("Server")) {
                calculateServerRotation(rotTarget);
            } else {
                rotateTowards(rotTarget);
            }
        }

        if (currentTarget == null) return; // only pre-aim, no attack

        // --- Crit logic (no forced jumping) ---
        if (critsMode.is("Packet") && isReadyToAttack()) {
            // Packet crit: send small Y offsets to fake falling
            sendCritPackets();
        } else if (critsMode.is("Smart")) {
            // Smart: attack only when already falling (natural crits)
            // Priority: crit from fall, but don't force jump
            if (!canPerformCrit() && isReadyToAttack()) {
                // If player is jumping naturally, wait for fall
                if (mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
                    return; // player is about to jump, wait for crit moment
                }
                // Otherwise attack anyway (don't force crits)
            }
        }

        if (!isReadyToAttack()) return;

        // Attack
        mc.interactionManager.attackEntity(mc.player, currentTarget);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();

        if (attackMode.is("1.8")) {
            double cps = ThreadLocalRandom.current().nextDouble(
                    Math.min(minCps.getValue(), maxCps.getValue()),
                    Math.max(minCps.getValue() + 0.1, maxCps.getValue()));
            currentDelay = (long) (1000.0 / cps);
        } else {
            currentDelay = 50;
        }
    }

    /** Send packet-based crit (small Y movement to simulate falling). */
    private void sendCritPackets() {
        if (mc.player == null || !mc.player.isOnGround()) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
        // Tiny Y offsets to trigger crit flag server-side
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 0.0625, z, yaw, pitch, false, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y + 1.1e-6, z, yaw, pitch, false, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, true, false));
    }

    /** Inject server-side rotation into outgoing movement packets. */
    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (!rotMode.is("Server") || !hasServerRotation) return;
        if (mc.player == null) return;
        if (currentTarget == null && scanTarget == null) return;

        if (event.getPacket() instanceof PlayerMoveC2SPacket.Full) {
            event.setPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    serverYaw, serverPitch,
                    mc.player.isOnGround(), mc.player.horizontalCollision));
        } else if (event.getPacket() instanceof PlayerMoveC2SPacket.LookAndOnGround) {
            event.setPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    serverYaw, serverPitch,
                    mc.player.isOnGround(), mc.player.horizontalCollision));
        }
    }

    /** Render range ring around player. */
    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (!rangeRing.isEnabled() || mc.player == null || currentTarget == null) return;
        DrawContext ctx = event.getDrawContext();

        Color themeColor = ThemeUtil.getThemeColor(0, ThemeType.GENERAL, 1);
        int color = ColorUtil.withAlpha(ColorUtil.toARGB(themeColor), 100);

        // Project ring points around player at attack range distance
        double r = range.getValue();
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int segments = 36;
        double[] lastScreen = null;

        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            double wx = playerPos.x + r * Math.cos(angle);
            double wz = playerPos.z + r * Math.sin(angle);
            double[] screen = RenderUtil.worldToScreen(wx, playerPos.y + 0.01, wz);

            if (screen != null && lastScreen != null && screen[2] > 0 && screen[2] < 1
                    && lastScreen[2] > 0 && lastScreen[2] < 1) {
                RenderUtil.drawSmoothLine(ctx, lastScreen[0], lastScreen[1],
                        screen[0], screen[1], 1.0f, color);
            }
            lastScreen = screen;
        }
    }

    private boolean isReadyToAttack() {
        if (System.currentTimeMillis() - lastAttackTime < currentDelay) return false;
        if (attackMode.is("1.9+")) {
            return mc.player.getAttackCooldownProgress(0.5f) >= (float) minCooldown.getValue();
        }
        return true;
    }

    private boolean canPerformCrit() {
        if (mc.player == null) return false;
        return !mc.player.isOnGround()
                && mc.player.fallDistance > 0
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing()
                && !mc.player.getAbilities().flying
                && mc.player.getVehicle() == null;
    }

    /** GCD-fixed server rotation — snaps to mouse sensitivity grid for AC bypass. */
    private void calculateServerRotation(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double diffY = (target.getY() + target.getEyeHeight(target.getPose()))
                - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) -(Math.toDegrees(Math.atan2(diffY, dist)));

        float speed = (float) rotSpeed.getValue();

        if (!hasServerRotation) {
            serverYaw = mc.player.getYaw();
            serverPitch = mc.player.getPitch();
            hasServerRotation = true;
        }

        float yawDiff = wrapAngle(targetYaw - serverYaw);
        float pitchDiff = targetPitch - serverPitch;

        // Apply speed
        float deltaYaw = yawDiff * speed;
        float deltaPitch = pitchDiff * speed;

        // Apply GCD fix — snap to sensitivity grid
        deltaYaw = applyGCD(deltaYaw);
        deltaPitch = applyGCD(deltaPitch);

        // Add slight randomization for AC bypass
        deltaYaw += (float)(ThreadLocalRandom.current().nextGaussian() * 0.08);
        deltaPitch += (float)(ThreadLocalRandom.current().nextGaussian() * 0.04);

        serverYaw += deltaYaw;
        serverPitch = Math.max(-90, Math.min(90, serverPitch + deltaPitch));
    }

    /** GCD fix: round to nearest sensitivity step. */
    private float applyGCD(float delta) {
        float gcd = getGCD();
        return Math.round(delta / gcd) * gcd;
    }

    private float getGCD() {
        float sens = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sens * 0.6f + 0.2f;
        return f * f * f * 1.2f;
    }

    private void rotateTowards(LivingEntity target) {
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double diffY = (target.getY() + target.getEyeHeight(target.getPose()))
                - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) -(Math.toDegrees(Math.atan2(diffY, dist)));

        float speed = (float) rotSpeed.getValue();
        float yawDiff = wrapAngle(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();

        mc.player.setYaw(mc.player.getYaw() + yawDiff * speed);
        mc.player.setPitch(Math.max(-90, Math.min(90, mc.player.getPitch() + pitchDiff * speed)));
    }

    private float wrapAngle(float angle) {
        angle %= 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    private LivingEntity findTarget(double maxRange) {
        LivingEntity best = null;
        double bestValue = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive()) continue;
            if (mc.player.distanceTo(living) > maxRange) continue;

            if (living instanceof PlayerEntity && !players.isEnabled()) continue;
            if (living instanceof Monster && !mobs.isEnabled()) continue;
            if (living instanceof AnimalEntity && !animals.isEnabled()) continue;
            if (!(living instanceof PlayerEntity) && !(living instanceof Monster) && !(living instanceof AnimalEntity)) continue;

            double value = switch (priority.getMode()) {
                case "Health" -> living.getHealth();
                case "Angle" -> getAngleTo(living);
                default -> mc.player.distanceTo(living);
            };

            if (value < bestValue) {
                bestValue = value;
                best = living;
            }
        }
        return best;
    }

    private double getAngleTo(Entity entity) {
        double diffX = entity.getX() - mc.player.getX();
        double diffZ = entity.getZ() - mc.player.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        return Math.abs(wrapAngle(mc.player.getYaw() - yaw));
    }

    @Override
    public String getSuffix() { return attackMode.getMode(); }

    public LivingEntity getCurrentTarget() { return currentTarget; }
}
