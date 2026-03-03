package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

/**
 * AutoShield — automatically uses shield when:
 * 1) An enemy player is within range
 * 2) KillAura is active and has a target
 * 3) Health is below threshold
 *
 * Unblocks briefly when KillAura needs to swing.
 */
@ModuleInfo(name = "AutoShield", description = "Auto-blocks with shield when threatened", category = Category.COMBAT)
public class AutoShield extends Module {

    private final NumberSetting range = new NumberSetting("Range", 4.0, 1.0, 8.0, 0.5);
    private final NumberSetting healthThreshold = new NumberSetting("Health Threshold", 10, 1, 20, 1);
    private final BooleanSetting killauraIntegration = new BooleanSetting("KillAura Sync", true);
    private final BooleanSetting onlyPlayers = new BooleanSetting("Only Players", true);

    private boolean isBlocking = false;
    private int unblockTicks = 0;

    public AutoShield() {
        addSettings(range, healthThreshold, killauraIntegration, onlyPlayers);
    }

    @Override
    protected void onDisable() {
        if (isBlocking && mc.player != null) {
            mc.interactionManager.stopUsingItem(mc.player);
            isBlocking = false;
        }
        unblockTicks = 0;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Check if player has shield
        boolean hasShield = mc.player.getOffHandStack().getItem() instanceof ShieldItem
                || mc.player.getMainHandStack().getItem() instanceof ShieldItem;
        if (!hasShield) {
            if (isBlocking) {
                isBlocking = false;
            }
            return;
        }

        // Brief unblock for KillAura attack window
        if (unblockTicks > 0) {
            unblockTicks--;
            return;
        }

        boolean shouldBlock = false;

        // Check KillAura integration
        if (killauraIntegration.isEnabled()) {
            KillAura killAura = ChaosClient.getInstance().getModuleManager().getModule(KillAura.class);
            if (killAura != null && killAura.isEnabled()) {
                LivingEntity target = killAura.getCurrentTarget();
                if (target != null && mc.player.distanceTo(target) <= range.getValue()) {
                    shouldBlock = true;
                    // If KillAura is about to attack, briefly unblock
                    if (mc.player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                        if (isBlocking) {
                            mc.interactionManager.stopUsingItem(mc.player);
                            isBlocking = false;
                            unblockTicks = 2; // unblock for 2 ticks for swing
                            return;
                        }
                    }
                }
            }
        }

        // Check for nearby threats
        if (!shouldBlock) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                if (onlyPlayers.isEnabled() && !(entity instanceof PlayerEntity)) continue;
                if (!(entity instanceof LivingEntity living)) continue;
                if (!living.isAlive()) continue;
                if (mc.player.distanceTo(living) <= range.getValue()) {
                    // Check if they're looking at us (swinging)
                    if (living.handSwinging || mc.player.getHealth() <= healthThreshold.getValue()) {
                        shouldBlock = true;
                        break;
                    }
                }
            }
        }

        // Low health — always block
        if (!shouldBlock && mc.player.getHealth() <= healthThreshold.getValue()) {
            shouldBlock = true;
        }

        if (shouldBlock && !isBlocking) {
            Hand blockHand = mc.player.getOffHandStack().getItem() instanceof ShieldItem
                    ? Hand.OFF_HAND : Hand.MAIN_HAND;
            mc.interactionManager.interactItem(mc.player, blockHand);
            isBlocking = true;
        } else if (!shouldBlock && isBlocking) {
            mc.interactionManager.stopUsingItem(mc.player);
            isBlocking = false;
        }
    }
}
