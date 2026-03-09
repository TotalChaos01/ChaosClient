package me.totalchaos01.chaosclient.util.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Server-side rotation management for anti-cheat bypass.
 * Tracks server-perceived yaw/pitch independently of client camera.
 * Provides GCD-fixed rotation calculation and smooth rotation utilities.
 *
 * === WHY THIS MATTERS FOR GRIM/MATRIX ===
 * Grim validates that rotation deltas align with the mouse sensitivity GCD grid.
 * Matrix checks for rotation consistency across ticks (no teleporting aim).
 * Both flag impossible rotation speeds (>180° instant snaps).
 * This manager ensures every outbound rotation is:
 *   1) GCD-grid aligned (matches real mouse input granularity)
 *   2) Smoothly interpolated (no instant snaps beyond configurable max speed)
 *   3) Consistent with previously sent rotations (delta tracking)
 */
public final class RotationUtil {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // --- Server-side rotation state (what the server thinks we're looking at) ---
    private static float serverYaw;
    private static float serverPitch;

    // --- Previous tick's server rotations (for delta-based checks) ---
    private static float prevServerYaw;
    private static float prevServerPitch;

    // --- Rotation management flags ---
    private static boolean rotating = false;
    private static int rotationTicks = 0;

    // --- Movement correction flag ---
    // When true AND rotating, the MixinKeyboardInput will rotate
    // the input vector from client-yaw space to server-yaw space.
    // This must be set by modules (KillAura, etc.) that want silent movement.
    private static boolean moveCorrectionEnabled = false;

    private RotationUtil() {}

    /**
     * Called every tick (from MixinMinecraftClient) BEFORE any module logic.
     * Syncs the "server rotation" with the real client camera when we're not
     * actively spoofing. This ensures Grim's delta tracking sees a seamless
     * transition when KillAura stops aiming.
     */
    public static void update() {
        if (mc.player == null) {
            rotating = false;
            return;
        }

        // Store previous for delta calculations
        prevServerYaw = serverYaw;
        prevServerPitch = serverPitch;

        if (!rotating) {
            // Not spoofing -> server rotations = client camera
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
     * Advanced smooth rotation with acceleration clamping and GCD alignment.
     * This is the primary method KillAura should use.
     *
     * === GRIM BYPASS LOGIC ===
     * Grim's RotationCheck compares your rotation delta each tick against a
     * maximum threshold. Vanilla mouse input is inherently limited by the
     * sensitivity GCD, so we:
     *   1) Clamp the per-tick delta to maxSpeed (prevents impossible snap flags)
     *   2) Apply GCD rounding (mimics mouse hardware quantization)
     *   3) Add micro-randomization (prevents pattern detection)
     *
     * @param targetYaw   desired yaw to aim at
     * @param targetPitch desired pitch to aim at
     * @param maxSpeed    max degrees per tick (35-45 is safe for Grim)
     * @param randomize   whether to add micro-randomization
     */
    public static void smoothRotate(float targetYaw, float targetPitch, float maxSpeed, boolean randomize) {
        float yawDiff = MathHelper.wrapDegrees(targetYaw - serverYaw);
        float pitchDiff = targetPitch - serverPitch;

        // Clamp per-tick rotation delta to maxSpeed
        // Grim flags any single-tick delta above ~45 degrees for combat scenarios
        yawDiff = MathHelper.clamp(yawDiff, -maxSpeed, maxSpeed);
        pitchDiff = MathHelper.clamp(pitchDiff, -maxSpeed * 0.7f, maxSpeed * 0.7f);

        float newYaw = serverYaw + yawDiff;
        float newPitch = MathHelper.clamp(serverPitch + pitchDiff, -90, 90);

        // Micro-randomization: noise mimics human hand tremor
        // Matrix's pattern detection looks for perfectly smooth curves
        if (randomize) {
            newYaw += (float) ((Math.random() - 0.5) * 0.7);
            newPitch += (float) ((Math.random() - 0.5) * 0.3);
            newPitch = MathHelper.clamp(newPitch, -90, 90);
        }

        // GCD fix: snap to mouse sensitivity grid
        float[] fixed = applyGCD(newYaw, newPitch);
        serverYaw = fixed[0];
        serverPitch = fixed[1];
        rotating = true;
        rotationTicks = 3;
    }

    /**
     * Legacy smooth rotate (backward compatibility).
     */
    public static void smoothRotate(float targetYaw, float targetPitch, float maxSpeed) {
        smoothRotate(targetYaw, targetPitch, maxSpeed, false);
    }

    /**
     * Calculate yaw and pitch from player eyes to an arbitrary world position.
     *
     * Uses atan2 for yaw (standard MC formula) and negative atan2 for pitch.
     * The -90 offset converts from math coordinates to MC's coordinate system
     * where 0 degrees yaw = south (+Z).
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
     * Calculate yaw/pitch to the best aim point on an entity.
     * Targets 0.85 * height (upper chest) for optimal hitreg without
     * extreme pitch values when close.
     */
    public static float[] getRotationsTo(Entity entity) {
        double eyeY = entity.getY() + entity.getHeight() * 0.85;
        return getRotationsTo(entity.getX(), eyeY, entity.getZ());
    }

    /**
     * Calculate rotations to the nearest visible point on entity's bounding box.
     * This finds the closest point on the entity AABB to the player's eye position,
     * which gives the smallest possible rotation delta (less movement = less flags).
     *
     * === GRIM BYPASS ===
     * By aiming at the nearest AABB point we minimize rotation delta while
     * still maintaining valid server-side aim.
     */
    public static float[] getRotationsToNearest(Entity entity) {
        if (mc.player == null) return new float[]{0, 0};

        Vec3d eyePos = mc.player.getEyePos();
        Box box = entity.getBoundingBox();

        // Clamp player eye position to the entity's bounding box
        double nearX = MathHelper.clamp(eyePos.x, box.minX, box.maxX);
        double nearY = MathHelper.clamp(eyePos.y, box.minY, box.maxY);
        double nearZ = MathHelper.clamp(eyePos.z, box.minZ, box.maxZ);

        return getRotationsTo(nearX, nearY, nearZ);
    }

    /**
     * Apply GCD (Greatest Common Divisor) fix to rotation values.
     *
     * === CRITICAL GRIM/MATRIX BYPASS ===
     * When the vanilla client processes mouse input, the sensitivity setting
     * creates a quantization grid. Every legitimate rotation change is a
     * multiple of: (sensitivity * 0.6 + 0.2)^3 * 1.2
     *
     * Grim's SensitivityProcessor reconstructs the player's sensitivity from
     * their rotation deltas. If our rotations don't snap to this grid,
     * Grim flags "InvalidRotation" immediately.
     */
    public static float[] applyGCD(float yaw, float pitch) {
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 1.2f;

        // Snap both axes to the GCD grid
        yaw = yaw - (yaw % gcd);
        pitch = pitch - (pitch % gcd);

        return new float[]{yaw, MathHelper.clamp(pitch, -90, 90)};
    }

    /**
     * Calculate the total angle difference between current look and target entity.
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

    /**
     * Check if server-side rotations are roughly aimed at the given entity.
     * Used to verify we've finished rotating before attacking.
     *
     * @param entity    target entity
     * @param tolerance max angle deviation in degrees
     * @return true if server yaw/pitch are within tolerance of the entity
     */
    public static boolean isAimingAt(Entity entity, float tolerance) {
        float[] target = getRotationsToNearest(entity);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(target[0] - serverYaw));
        float pitchDiff = Math.abs(target[1] - serverPitch);
        return (yawDiff + pitchDiff) < tolerance;
    }

    /**
     * Get the rotation delta from the previous tick. Useful for checking
     * if our rotation speed is within safe bounds.
     */
    public static float getRotationDelta() {
        float yawDelta = Math.abs(MathHelper.wrapDegrees(serverYaw - prevServerYaw));
        float pitchDelta = Math.abs(serverPitch - prevServerPitch);
        return yawDelta + pitchDelta;
    }

    // --- Accessors ---

    public static float getServerYaw() { return serverYaw; }
    public static float getServerPitch() { return serverPitch; }
    public static float getPrevServerYaw() { return prevServerYaw; }
    public static float getPrevServerPitch() { return prevServerPitch; }
    public static boolean isRotating() { return rotating; }

    public static void setMoveCorrection(boolean enabled) { moveCorrectionEnabled = enabled; }
    public static boolean isMoveFixActive() { return rotating && moveCorrectionEnabled; }

    /**
     * Apply movement correction: rotate the input vector from client-yaw to server-yaw space.
     * Called from MixinKeyboardInput AFTER Input.tick() computes the raw movementVector.
     *
     * Math: We want the player to move as if looking at serverYaw, but the client camera
     * is at clientYaw. So we rotate the input vector by (serverYaw - clientYaw).
     * When MC later applies clientYaw to this rotated vector, the result = serverYaw applied to the original.
     *
     * This is what makes "silent movement" work: WASD moves you toward the server
     * rotation (target), not toward where the camera is pointing.
     */
    public static void correctMovement(net.minecraft.client.input.Input input, float clientYaw) {
        if (!isMoveFixActive()) return;

        float yawDiff = MathHelper.wrapDegrees(serverYaw - clientYaw);
        if (Math.abs(yawDiff) < 1.0f) return;

        net.minecraft.util.math.Vec2f moveVec = input.movementVector;
        float forward = moveVec.y;
        float strafe = moveVec.x;
        if (forward == 0 && strafe == 0) return;

        double rad = Math.toRadians(yawDiff);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float newForward = forward * cos + strafe * sin;
        float newStrafe = strafe * cos - forward * sin;

        input.movementVector = new net.minecraft.util.math.Vec2f(newStrafe, newForward);
    }

    public static void reset() {
        rotating = false;
        rotationTicks = 0;
    }
}
