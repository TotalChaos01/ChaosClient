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
import net.minecraft.util.Hand;

@ModuleInfo(name = "KillAura", description = "Automatically attacks nearby entities", category = Category.COMBAT)
public class KillAura extends Module {

    private final NumberSetting range = new NumberSetting("Range", 3.5, 1.0, 6.0, 0.1);
    private final NumberSetting cps = new NumberSetting("CPS", 12, 1, 20, 1);
    private final ModeSetting priority = new ModeSetting("Priority", "Distance", "Distance", "Health", "Angle");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    private final BooleanSetting autoBlock = new BooleanSetting("AutoBlock", false);

    private long lastAttackTime;

    public KillAura() {
        addSettings(range, cps, priority, players, mobs, animals, autoBlock);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        long delay = (long) (1000.0 / cps.getValue());
        if (System.currentTimeMillis() - lastAttackTime < delay) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttackTime = System.currentTimeMillis();
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
        float diff = Math.abs(mc.player.getYaw() - yaw) % 360;
        return diff > 180 ? 360 - diff : diff;
    }

    @Override
    public String getSuffix() {
        return priority.getMode();
    }
}

