package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

@ModuleInfo(name = "TriggerBot", description = "Auto attack when target enters reach", category = Category.LEGIT)
public class TriggerBot extends Module {

    private final BooleanSetting onlyCrits = new BooleanSetting("Only Crits", false);
    private final BooleanSetting useCooldown = new BooleanSetting("Use Cooldown", true);

    private final NumberSetting minCps = new NumberSetting("Min CPS", 9, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 12, 1, 20, 1);

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting invisible = new BooleanSetting("Invisible", false);
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon Only", false);

    private long nextAttackAt;

    public TriggerBot() {
        minCps.setVisibility(() -> !useCooldown.isEnabled());
        maxCps.setVisibility(() -> !useCooldown.isEnabled());
        addSettings(onlyCrits, useCooldown, minCps, maxCps, players, mobs, invisible, weaponOnly);
    }

    @EventTarget
    public void onTick(EventTick ignoredEvent) {
        if (mc.player == null || mc.world == null) return;

        if (useCooldown.isEnabled()) {
            if (mc.player.getAttackCooldownProgress(0.0f) < 0.92f) return;
        } else if (System.currentTimeMillis() < nextAttackAt) {
            return;
        }

        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;

        Entity target = ehr.getEntity();
        if (!isValidTarget(target)) return;

        if (weaponOnly.isEnabled()) {
            if (mc.player.getMainHandStack().isEmpty()) return;
        }

        if (onlyCrits.isEnabled() && !canCritNow()) return;

        float baseReach = 3.0f;
        Reach reach = Reach.get();
        float reachBonus = (reach != null && reach.isEnabled()) ? reach.getValue() : 0.0f;
        double maxReach = baseReach + reachBonus;
        if (mc.player.distanceTo(target) > maxReach) return;

        mc.doAttack();

        if (!useCooldown.isEnabled()) {
            scheduleNextAttack();
        }
    }

    private boolean canCritNow() {
        if (mc.player == null) return false;
        return !mc.player.isOnGround()
                && mc.player.fallDistance > 0.0f
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing();
    }

    private void scheduleNextAttack() {
        int min = (int) minCps.getValue();
        int max = (int) maxCps.getValue();
        if (max < min) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        int cps = min + (int) (Math.random() * (max - min + 1));
        long delay = (long) (1000.0 / Math.max(1, cps));
        nextAttackAt = System.currentTimeMillis() + delay;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) return false;
        if (!invisible.isEnabled() && entity.isInvisible()) return false;
        if (entity instanceof PlayerEntity) return players.isEnabled();
        if (entity instanceof MobEntity) return mobs.isEnabled();
        return false;
    }

    @Override
    protected void onEnable() {
        nextAttackAt = 0L;
    }
}
