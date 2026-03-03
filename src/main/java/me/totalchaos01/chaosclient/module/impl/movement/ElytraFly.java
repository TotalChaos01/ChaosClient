package me.totalchaos01.chaosclient.module.impl.movement;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

/**
 * ElytraFly — enhanced elytra flight with multiple modes.
 */
@ModuleInfo(name = "ElytraFly", description = "Enhanced elytra flight", category = Category.MOVEMENT)
public class ElytraFly extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Control", "Control", "Boost", "Firework", "Packet");
    private final NumberSetting speed = new NumberSetting("Speed", 1.8, 0.1, 5.0, 0.1);
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", 1.0, 0.1, 3.0, 0.1);
    private final BooleanSetting lockY = new BooleanSetting("Lock Y", false);
    private final BooleanSetting autoStart = new BooleanSetting("Auto Start", true);
    private final BooleanSetting antiKick = new BooleanSetting("Anti Kick", true);

    private int antiKickTicks = 0;

    public ElytraFly() {
        addSettings(mode, speed, verticalSpeed, lockY, autoStart, antiKick);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        // Auto-start elytra flying
        if (autoStart.isEnabled() && !mc.player.isGliding() && mc.player.fallDistance > 0.5 && !mc.player.isOnGround()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }

        if (!mc.player.isGliding()) return;

        switch (mode.getMode()) {
            case "Control" -> handleControl();
            case "Boost" -> handleBoost();
            case "Packet" -> handlePacket();
        }

        // Anti-kick: small downward movement to prevent kick
        if (antiKick.isEnabled()) {
            antiKickTicks++;
            if (antiKickTicks >= 40) {
                antiKickTicks = 0;
                mc.player.setVelocity(mc.player.getVelocity().x, -0.04, mc.player.getVelocity().z);
            }
        }
    }

    private void handleControl() {
        double yaw = Math.toRadians(mc.player.getYaw());
        double pitch = Math.toRadians(mc.player.getPitch());
        double spd = speed.getValue();
        double vSpd = verticalSpeed.getValue();

        double motionX = 0, motionY = 0, motionZ = 0;

        if (mc.options.forwardKey.isPressed()) {
            motionX -= Math.sin(yaw) * spd;
            motionZ += Math.cos(yaw) * spd;
        }
        if (mc.options.backKey.isPressed()) {
            motionX += Math.sin(yaw) * spd;
            motionZ -= Math.cos(yaw) * spd;
        }

        if (lockY.isEnabled()) {
            if (mc.options.jumpKey.isPressed()) motionY = vSpd;
            else if (mc.options.sneakKey.isPressed()) motionY = -vSpd;
            else motionY = 0;
        } else {
            if (mc.options.jumpKey.isPressed()) motionY = vSpd;
            else if (mc.options.sneakKey.isPressed()) motionY = -vSpd;
            else motionY = mc.player.getVelocity().y * 0.99;
        }

        mc.player.setVelocity(motionX, motionY, motionZ);
        mc.player.getAbilities().flying = false;
    }

    private void handleBoost() {
        double yaw = Math.toRadians(mc.player.getYaw());
        double pitch = Math.toRadians(mc.player.getPitch());
        double spd = speed.getValue() * 0.05;

        Vec3d vel = mc.player.getVelocity();

        if (mc.options.forwardKey.isPressed()) {
            mc.player.addVelocity(-Math.sin(yaw) * spd, 0, Math.cos(yaw) * spd);
        }

        // Clamp speed
        double currentSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (currentSpeed > speed.getValue()) {
            double factor = speed.getValue() / currentSpeed;
            mc.player.setVelocity(vel.x * factor, vel.y, vel.z * factor);
        }
    }

    private void handlePacket() {
        double yaw = Math.toRadians(mc.player.getYaw());
        double spd = speed.getValue();

        double motionX = 0, motionY = 0, motionZ = 0;

        if (mc.options.forwardKey.isPressed()) {
            motionX = -Math.sin(yaw) * spd;
            motionZ = Math.cos(yaw) * spd;
        }

        if (mc.options.jumpKey.isPressed()) motionY = verticalSpeed.getValue();
        else if (mc.options.sneakKey.isPressed()) motionY = -verticalSpeed.getValue();

        mc.player.setVelocity(motionX, motionY, motionZ);

        // Send position packets
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, mc.player.horizontalCollision
        ));
    }

    @Override
    protected void onDisable() {
        antiKickTicks = 0;
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }
}
