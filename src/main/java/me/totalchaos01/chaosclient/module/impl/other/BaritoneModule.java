package me.totalchaos01.chaosclient.module.impl.other;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.*;
import baritone.api.process.ICustomGoalProcess;
import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * Baritone integration module — exposes key Baritone settings through ChaosClient's GUI.
 * Baritone itself is fully embedded; this module controls its behaviour.
 * Use the .b / .baritone chat command for direct Baritone commands.
 */
@ModuleInfo(name = "Baritone", description = "Embedded pathfinding bot (Baritone)", category = Category.MOVEMENT)
public class BaritoneModule extends Module {

    /* ── GUI-exposed settings ─────────────────────────────── */
    private final BooleanSetting allowSprint   = new BooleanSetting("Allow Sprint", true);
    private final BooleanSetting allowParkour  = new BooleanSetting("Allow Parkour", true);
    private final BooleanSetting allowBreak    = new BooleanSetting("Allow Break", true);
    private final BooleanSetting allowPlace    = new BooleanSetting("Allow Place", true);
    private final BooleanSetting avoidance     = new BooleanSetting("Mob Avoidance", true);
    private final BooleanSetting autoTool      = new BooleanSetting("Auto Tool", true);
    private final BooleanSetting renderPath    = new BooleanSetting("Render Path", true);
    private final BooleanSetting renderGoal    = new BooleanSetting("Render Goal", true);
    private final BooleanSetting freeLook      = new BooleanSetting("Free Look", true);
    private final NumberSetting  followRadius  = new NumberSetting("Follow Radius", 3, 1, 20, 1);
    private final NumberSetting  blockReach    = new NumberSetting("Block Reach", 4.5, 1, 6, 0.5);

    public BaritoneModule() {
        addSettings(
                allowSprint, allowParkour, allowBreak, allowPlace,
                avoidance, autoTool, renderPath, renderGoal, freeLook,
                followRadius, blockReach
        );
    }

    /* ── Lifecycle ────────────────────────────────────────── */

    @Override
    protected void onEnable() {
        syncSettings();
    }

    @Override
    protected void onDisable() {
        // Stop any active pathing when module is toggled off
        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (baritone != null) {
                baritone.getPathingBehavior().cancelEverything();
            }
        } catch (Exception e) {
            ChaosClient.LOGGER.warn("Failed to cancel Baritone pathing", e);
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        syncSettings();
    }

    /* ── Settings sync ────────────────────────────────────── */

    /**
     * Pushes ChaosClient GUI values → Baritone Settings every tick.
     */
    private void syncSettings() {
        try {
            Settings s = BaritoneAPI.getSettings();
            s.allowSprint.value            = allowSprint.isEnabled();
            s.allowParkour.value           = allowParkour.isEnabled();
            s.allowBreak.value             = allowBreak.isEnabled();
            s.allowPlace.value             = allowPlace.isEnabled();
            s.avoidance.value              = avoidance.isEnabled();
            s.autoTool.value               = autoTool.isEnabled();
            s.renderPath.value             = renderPath.isEnabled();
            s.renderGoal.value             = renderGoal.isEnabled();
            s.freeLook.value               = freeLook.isEnabled();
            s.followRadius.value           = (int) followRadius.getValue();
            s.blockReachDistance.value      = (float) blockReach.getValue();
            // Disable Baritone's own chat control — we wrap it via .b command
            s.chatControl.value            = false;
            s.chatControlAnyway.value      = false;
            s.prefixControl.value          = false;
        } catch (Exception e) {
            // Baritone not yet initialised — ignore
        }
    }

    /* ── Public helpers for other modules / commands ─────── */

    /**
     * Walk to a specific coordinate.
     */
    public static void goTo(int x, int y, int z) {
        IBaritone b = primary();
        if (b == null) return;
        b.getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));
    }

    /**
     * Walk to X/Z (any Y).
     */
    public static void goToXZ(int x, int z) {
        IBaritone b = primary();
        if (b == null) return;
        b.getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
    }

    /**
     * Mine a block by registry name, e.g. "diamond_ore".
     */
    public static void mine(String... blockNames) {
        IBaritone b = primary();
        if (b == null) return;
        b.getMineProcess().mineByName(0, blockNames);
    }

    /**
     * Follow the nearest player by name.
     */
    public static void follow(String playerName) {
        IBaritone b = primary();
        if (b == null) return;
        b.getFollowProcess().follow(entity ->
                entity instanceof net.minecraft.entity.player.PlayerEntity p
                        && p.getName().getString().equalsIgnoreCase(playerName));
    }

    /**
     * Follow all nearby players.
     */
    public static void followAll() {
        IBaritone b = primary();
        if (b == null) return;
        b.getFollowProcess().follow(entity ->
                entity instanceof net.minecraft.entity.player.PlayerEntity
                        && entity != net.minecraft.client.MinecraftClient.getInstance().player);
    }

    /**
     * Explore from the player's current position.
     */
    public static void explore() {
        IBaritone b = primary();
        if (b == null) return;
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null) return;
        b.getExploreProcess().explore((int) player.getX(), (int) player.getZ());
    }

    /**
     * Farm in radius around player.
     */
    public static void farm(int radius) {
        IBaritone b = primary();
        if (b == null) return;
        b.getFarmProcess().farm(radius, null);
    }

    /**
     * Cancel all active Baritone processes.
     */
    public static void stop() {
        IBaritone b = primary();
        if (b == null) return;
        b.getPathingBehavior().cancelEverything();
    }

    /**
     * Check if Baritone is currently pathing.
     */
    public static boolean isPathing() {
        IBaritone b = primary();
        return b != null && b.getPathingBehavior().isPathing();
    }

    /**
     * Get the current active Baritone goal, or null.
     */
    public static baritone.api.pathing.goals.Goal getGoal() {
        IBaritone b = primary();
        return b == null ? null : b.getPathingBehavior().getGoal();
    }

    private static IBaritone primary() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Exception e) {
            return null;
        }
    }
}
