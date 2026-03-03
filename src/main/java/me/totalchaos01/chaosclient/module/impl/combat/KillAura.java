package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Advanced KillAura with AutoBlock (shield), smooth rotation,
 * randomized CPS, and multi-priority targeting.
 */
@ModuleInfo(name = "KillAura", description = "Automatically attacks nearby entities", category = Category.COMBAT)
public class KillAura extends Module {

    private final NumberSetting range = new NumberSetting("Range", 3.5, 1.0, 6.0, 0.1);
    private final NumberSetting minCps = new NumberSetting("Min CPS", 10, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 14, 1, 20, 1);
    private final ModeSetting priority = new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    private final BooleanSetting autoBlock = new BooleanSetting("AutoBlock", false);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    private final NumberSetting rotSpeed = new NumberSetting("Rot Speed", 0.6, 0.1, 1.0, 0.05);

    private long lastAttackTime;
    private long currentDelay;
    private LivingEntity currentTarget;
    private boolean blocking;

    public KillAura() {
        addSettings(range, minCps, maxCps, priority, players, mobs, animals, autoBlock, rotate, rotSpeed);
        currentDelay = 80;
    }

    @Override
    public void onDisable() {
        if (blocking && mc.player != null) {
            mc.interactionManager.stopUsingItem(mc.player);
            blocking = false;
        }
        currentTarget = null;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        currentTarget = findTarget();

        if (currentTarget == null) {
            // Stop blocking if no target
            if (blocking && autoBlock.isEnabled()) {
                mc.interactionManager.stopUsingItem(mc.player);
                blocking = false;
            }
            return;
        }

        // Smooth rotation towards target
        if (rotate.isEnabled()) {
            rotateTowards(currentTarget);
        }

        // AutoBlock — use shield when target is nearby
        if (autoBlock.isEnabled()) {
            boolean hasShield = mc.player.getOffHandStack().getItem() instanceof ShieldItem
                    || mc.player.getMainHandStack().getItem() instanceof ShieldItem;
            if (hasShield) {
                if (!blocking) {
                    // Start using shield
                    Hand blockHand = mc.player.getOffHandStack().getItem() instanceof ShieldItem
                            ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    mc.interactionManager.interactItem(mc.player, blockHand);
                    blocking = true;
                }
            }
        }

        // Attack with randomized CPS
        if (System.currentTimeMillis() - lastAttackTime < currentDelay) return;

        // Unblock briefly to attack
        if (blocking) {
            mc.interactionManager.stopUsingItem(mc.player);
        }

        mc.interactionManager.attackEntity(mc.player, currentTarget);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();

        // Randomize next delay
        double cps = ThreadLocalRandom.current().nextDouble(
                Math.min(minCps.getValue(), maxCps.getValue()),
                Math.max(minCps.getValue() + 0.1, maxCps.getValue()));
        currentDelay = (long) (1000.0 / cps);

        // Re-block after attack
        if (blocking && autoBlock.isEnabled()) {
            Hand blockHand = mc.player.getOffHandStack().getItem() instanceof ShieldItem
                    ? Hand.OFF_HAND : Hand.MAIN_HAND;
            mc.interactionManager.interactItem(mc.player, blockHand);
        }
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

    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestValue = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive()) continue;
            if (mc.player.distanceTo(living) > range.getValue()) continue;

            if (living instanceof PlayerEntity && !players.isEnabled()) continue;
            if (living instanceof Monster && !mobs.isEnabled()) continue;
            if (living instanceof AnimalEntity && !animals.isEnabled()) continue;

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
        float diff = Math.abs(wrapAngle(mc.player.getYaw() - yaw));
        return diff;
    }

    @Override
    public String getSuffix() {
        return priority.getMode();
    }
}

