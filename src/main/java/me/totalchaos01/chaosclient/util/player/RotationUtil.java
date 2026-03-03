package me.totalchaos01.chaosclient.util.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/**
 * Server-side rotation management for anti-cheat bypass.
 * Tracks server-perceived yaw/pitch independently of client camera.
 * Provides GCD-fixed rotation calculation and smooth rotation utilities.
 */
public final class RotationUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static float serverYaw;
    private static float serverPitch;
    private static boolean rotating = false;
    private static int rotationTicks = 0;

    private RotationUtil() {}

    /**
     * Call each tick to update rotation state.
     * Syncs server rotation with client when not actively spoofing.
     */
    public static void update() {
        if (mc.player == null) {
            rotating = false;
            return;
        }
        if (!rotating) {
            serverYaw = mc.player.getYaw();
            serverPitch = mc.player.getPitch();
        }
        if (rotationTicks > 0) {
            rotationTicks--;
            if (rotationTicks == 0) {
                rotating = false;
            }
        }
    }

    /**
     * Set server-side rotation directly (silent aim).
     * @param yaw target yaw
     * @param pitch target pitch
     * @param holdTicks how many ticks to hold this rotation
     */
    public static void setRotation(float yaw, float pitch, int holdTicks) {
        serverYaw = yaw;
        serverPitch = MathHelper.clamp(pitch, -90, 90);
        rotating = true;
        rotationTicks = holdTicks;
    }

    /**
     * Smoothly rotate towards a target direction over multiple ticks.
     * @param targetYaw destination yaw
     * @param targetPitch destination pitch
     * @param maxSpeed max degrees per tick
     */
    public static void smoothRotate(float targetYaw, float targetPitch, float maxSpeed) {
        float yawDiff = MathHelper.wrapDegrees(targetYaw - serverYaw);
        float pitchDiff = targetPitch - serverPitch;

        yawDiff = MathHelper.clamp(yawDiff, -maxSpeed, maxSpeed);
        pitchDiff = MathHelper.clamp(pitchDiff, -maxSpeed, maxSpeed);

        serverYaw += yawDiff;
        serverPitch = MathHelper.clamp(serverPitch + pitchDiff, -90, 90);
        rotating = true;
        rotationTicks = 3;
    }

    /**
     * Calculate yaw and pitch to look at a world position from the player's eye.
     */
    public static float[] getRotationsTo(double x, double y, double z) {
        if (mc.player == null) return new float[]{0, 0};
        double dx = x - mc.player.getX();
        double dy = y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = z - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{yaw, MathHelper.clamp(pitch, -90, 90)};
    }

    /**
     * Calculate yaw and pitch to look at an entity's upper body.
     */
    public static float[] getRotationsTo(Entity entity) {
        double eyeY = entity.getY() + entity.getHeight() * 0.85;
        return getRotationsTo(entity.getX(), eyeY, entity.getZ());
    }

    /**
     * Apply GCD (Greatest Common Divisor) fix to rotation values.
     * Required to bypass server-side rotation consistency checks (e.g. Grim).
     * Snaps rotations to the mouse sensitivity grid.
     */
    public static float[] applyGCD(float yaw, float pitch) {
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 1.2f;

        yaw -= (yaw % gcd);
        pitch -= (pitch % gcd);

        return new float[]{yaw, MathHelper.clamp(pitch, -90, 90)};
    }

    /**
     * Calculate the total angle difference between current look and target entity.
     * Useful for aim assist FOV checks.
     */
    public static float getAngleDifference(Entity entity) {
        if (mc.player == null) return 180;
        float[] target = getRotationsTo(entity);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(target[0] - mc.player.getYaw()));
        float pitchDiff = Math.abs(target[1] - mc.player.getPitch());
        return yawDiff + pitchDiff;
    }

    /**
     * Calculate yaw difference between current look and an entity.
     */
    public static float getYawDifference(Entity entity) {
        if (mc.player == null) return 180;
        float[] target = getRotationsTo(entity);
        return Math.abs(MathHelper.wrapDegrees(target[0] - mc.player.getYaw()));
    }

    // --- Accessors ---

    public static float getServerYaw() { return serverYaw; }
    public static float getServerPitch() { return serverPitch; }
    public static boolean isRotating() { return rotating; }

    public static void reset() {
        rotating = false;
        rotationTicks = 0;
    }
}
