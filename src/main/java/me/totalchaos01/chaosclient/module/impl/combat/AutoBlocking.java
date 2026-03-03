package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

/**
 * AutoBlocking 1.8 — simulates 1.8-style sword blocking by rapidly
 * right-clicking with a sword or shield between attacks.
 * Modes:
 * - Fake: sends block/unblock packets around attacks for visual effect
 * - Interact: uses interact entity packet-based blocking
 */
@ModuleInfo(name = "AutoBlocking", description = "1.8-style auto-blocking between attacks", category = Category.COMBAT)
public class AutoBlocking extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Fake", "Fake", "Interact");

    private boolean blocking = false;

    public AutoBlocking() {
        addSettings(mode);
    }

    @Override
    protected void onDisable() {
        if (blocking && mc.player != null) {
            mc.interactionManager.stopUsingItem(mc.player);
            blocking = false;
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Check if KillAura has an active target
        KillAura ka = ChaosClient.getInstance().getModuleManager().getModule(KillAura.class);
        if (ka == null || !ka.isEnabled() || ka.getCurrentTarget() == null) {
            if (blocking) {
                mc.interactionManager.stopUsingItem(mc.player);
                blocking = false;
            }
            return;
        }

        LivingEntity target = ka.getCurrentTarget();

        if (mode.is("Fake")) {
            // Fake blocking: use shield/sword in offhand between attacks
            if (!blocking) {
                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                blocking = true;
            }
        } else if (mode.is("Interact")) {
            // Interact mode: interact with target entity for visual block
            if (!blocking) {
                mc.interactionManager.interactEntity(mc.player, target, Hand.OFF_HAND);
                blocking = true;
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
