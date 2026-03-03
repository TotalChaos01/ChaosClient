package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;

/**
 * LongJump — extends the distance and/or height of jumps.
 */
@ModuleInfo(name = "LongJump", description = "Extends jump distance", category = Category.MOVEMENT)
public class LongJump extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "NCP", "Boost");
    private final NumberSetting boostSpeed = new NumberSetting("Boost", 1.5, 0.5, 5.0, 0.1);
    private final NumberSetting jumpHeight = new NumberSetting("Jump Height", 0.42, 0.3, 1.0, 0.01);
    private final BooleanSetting autoDisable = new BooleanSetting("Auto Disable", true);

    private boolean jumped = false;

    public LongJump() {
        addSettings(mode, boostSpeed, jumpHeight, autoDisable);
    }

    @Override
    protected void onEnable() {
        jumped = false;
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        switch (mode.getMode()) {
            case "Vanilla" -> handleVanilla();
            case "NCP" -> handleNCP();
            case "Boost" -> handleBoost();
        }
    }

    private void handleVanilla() {
        if (mc.player.isOnGround()) {
            if (jumped && autoDisable.isEnabled()) {
                toggle();
                return;
            }
            mc.player.jump();
            jumped = true;
            double yaw = Math.toRadians(mc.player.getYaw());
            double speed = boostSpeed.getValue();
            mc.player.setVelocity(
                -Math.sin(yaw) * speed,
                jumpHeight.getValue(),
                Math.cos(yaw) * speed
            );
        }
    }

    private void handleNCP() {
        if (mc.player.isOnGround()) {
            if (jumped && autoDisable.isEnabled()) {
                toggle();
                return;
            }
            jumped = true;
            mc.player.jump();
            double yaw = Math.toRadians(mc.player.getYaw());
            mc.player.setVelocity(
                -Math.sin(yaw) * boostSpeed.getValue() * 0.8,
                jumpHeight.getValue(),
                Math.cos(yaw) * boostSpeed.getValue() * 0.8
            );
        } else {
            // In air - maintain speed
            double yaw = Math.toRadians(mc.player.getYaw());
            double currentSpeed = Math.sqrt(
                mc.player.getVelocity().x * mc.player.getVelocity().x +
                mc.player.getVelocity().z * mc.player.getVelocity().z
            );
            if (currentSpeed > 0.05) {
                mc.player.setVelocity(
                    -Math.sin(yaw) * currentSpeed,
                    mc.player.getVelocity().y,
                    Math.cos(yaw) * currentSpeed
                );
            }
        }
    }

    private void handleBoost() {
        if (mc.player.isOnGround()) {
            if (jumped && autoDisable.isEnabled()) {
                toggle();
                return;
            }
            jumped = true;
            mc.player.setVelocity(mc.player.getVelocity().x, jumpHeight.getValue(), mc.player.getVelocity().z);
        }

        if (!mc.player.isOnGround() && jumped) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double boost = boostSpeed.getValue() * 0.05;
            mc.player.addVelocity(-Math.sin(yaw) * boost, 0, Math.cos(yaw) * boost);
        }
    }

    @Override
    protected void onDisable() {
        jumped = false;
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
