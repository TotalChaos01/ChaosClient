package me.totalchaos01.chaosclient.command.impl;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.*;
import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.command.CommandManager;
import me.totalchaos01.chaosclient.module.impl.other.BaritoneModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chat command that wraps Baritone's pathfinding features.
 * Usage:
 *   .b goto <x> <y> <z>       — path to coords
 *   .b goto <x> <z>           — path to X/Z (any Y)
 *   .b mine <block> [block..] — mine specific blocks
 *   .b follow <player|all>    — follow a player
 *   .b farm [radius]          — auto-farm nearby crops
 *   .b explore                — explore the world
 *   .b stop                   — cancel all pathing
 *   .b status                 — show current state
 *   .b elytra <x> <z>         — elytra fly to coords
 *   .b thisway <distance>     — go in looking direction
 *   .b <raw command>          — pass raw text to Baritone
 */
public class BaritoneCommand extends Command {

    private static final String[] SUB_COMMANDS = {
            "goto", "mine", "follow", "farm", "explore", "stop",
            "status", "elytra", "thisway", "help", "cancel",
            "sel", "set", "goal"
    };

    public BaritoneCommand() {
        super("baritone", "Embedded Baritone pathfinding", ".b <subcommand> [args]", "b", "path");
    }

    @Override
    public void execute(String[] args) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            CommandManager.sendMessage("§cYou must be in a world!");
            return;
        }

        if (args.length == 0) {
            showHelp();
            return;
        }

        String sub = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (sub) {
            case "goto", "go" -> handleGoto(subArgs);
            case "mine" -> handleMine(subArgs);
            case "follow" -> handleFollow(subArgs);
            case "farm" -> handleFarm(subArgs);
            case "explore" -> handleExplore();
            case "stop", "cancel" -> handleStop();
            case "status" -> handleStatus();
            case "elytra" -> handleElytra(subArgs);
            case "thisway" -> handleThisWay(subArgs);
            case "goal" -> handleGoal(subArgs);
            case "set" -> handleSet(subArgs);
            case "help" -> showHelp();
            default -> handleRaw(args);
        }
    }

    /* ── Subcommand handlers ─────────────────────────────── */

    private void handleGoto(String[] args) {
        if (args.length == 2) {
            try {
                int x = Integer.parseInt(args[0]);
                int z = Integer.parseInt(args[1]);
                BaritoneModule.goToXZ(x, z);
                CommandManager.sendMessage("§aBaritone: pathing to X=" + x + " Z=" + z);
            } catch (NumberFormatException e) {
                CommandManager.sendMessage("§cUsage: .b goto <x> <z>");
            }
        } else if (args.length == 3) {
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                BaritoneModule.goTo(x, y, z);
                CommandManager.sendMessage("§aBaritone: pathing to " + x + " " + y + " " + z);
            } catch (NumberFormatException e) {
                CommandManager.sendMessage("§cUsage: .b goto <x> <y> <z>");
            }
        } else {
            CommandManager.sendMessage("§cUsage: .b goto <x> [y] <z>");
        }
    }

    private void handleMine(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("§cUsage: .b mine <block> [block2 ...]");
            return;
        }
        BaritoneModule.mine(args);
        CommandManager.sendMessage("§aBaritone: mining " + String.join(", ", args));
    }

    private void handleFollow(String[] args) {
        if (args.length == 0) {
            CommandManager.sendMessage("§cUsage: .b follow <player|all>");
            return;
        }
        if (args[0].equalsIgnoreCase("all")) {
            BaritoneModule.followAll();
            CommandManager.sendMessage("§aBaritone: following all players");
        } else {
            BaritoneModule.follow(args[0]);
            CommandManager.sendMessage("§aBaritone: following " + args[0]);
        }
    }

    private void handleFarm(String[] args) {
        int radius = 64;
        if (args.length >= 1) {
            try {
                radius = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                CommandManager.sendMessage("§cInvalid radius: " + args[0]);
                return;
            }
        }
        BaritoneModule.farm(radius);
        CommandManager.sendMessage("§aBaritone: farming (radius " + radius + ")");
    }

    private void handleExplore() {
        BaritoneModule.explore();
        CommandManager.sendMessage("§aBaritone: exploring...");
    }

    private void handleStop() {
        BaritoneModule.stop();
        CommandManager.sendMessage("§cBaritone: stopped");
    }

    private void handleStatus() {
        boolean pathing = BaritoneModule.isPathing();
        var goal = BaritoneModule.getGoal();
        String goalStr = goal != null ? goal.toString() : "none";
        CommandManager.sendMessage("§7Baritone status: " +
                (pathing ? "§aPathing" : "§cIdle") +
                " §7| Goal: §f" + goalStr);
    }

    private void handleElytra(String[] args) {
        if (args.length < 2) {
            CommandManager.sendMessage("§cUsage: .b elytra <x> <z>");
            return;
        }
        try {
            int x = Integer.parseInt(args[0]);
            int z = Integer.parseInt(args[1]);
            IBaritone b = BaritoneAPI.getProvider().getPrimaryBaritone();
            b.getElytraProcess().pathTo(new GoalXZ(x, z));
            CommandManager.sendMessage("§aBaritone: elytra pathing to X=" + x + " Z=" + z);
        } catch (NumberFormatException e) {
            CommandManager.sendMessage("§cUsage: .b elytra <x> <z>");
        } catch (Exception e) {
            CommandManager.sendMessage("§cElytra error: " + e.getMessage());
        }
    }

    private void handleThisWay(String[] args) {
        if (args.length < 1) {
            CommandManager.sendMessage("§cUsage: .b thisway <distance>");
            return;
        }
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            double distance = Double.parseDouble(args[0]);
            float yaw = mc.player.getYaw();
            double rad = Math.toRadians(yaw);
            int x = (int) (mc.player.getX() - Math.sin(rad) * distance);
            int z = (int) (mc.player.getZ() + Math.cos(rad) * distance);
            BaritoneModule.goToXZ(x, z);
            CommandManager.sendMessage("§aBaritone: going " + (int) distance + " blocks this way (X=" + x + " Z=" + z + ")");
        } catch (NumberFormatException e) {
            CommandManager.sendMessage("§cInvalid distance: " + args[0]);
        }
    }

    private void handleGoal(String[] args) {
        if (args.length == 0) {
            // Set goal to current goal without pathing
            var goal = BaritoneModule.getGoal();
            CommandManager.sendMessage("§7Current goal: " + (goal != null ? goal.toString() : "none"));
            return;
        }
        try {
            IBaritone b = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (args.length == 1) {
                int y = Integer.parseInt(args[0]);
                b.getCustomGoalProcess().setGoal(new GoalYLevel(y));
                CommandManager.sendMessage("§aGoal set: Y=" + y);
            } else if (args.length == 2) {
                int x = Integer.parseInt(args[0]);
                int z = Integer.parseInt(args[1]);
                b.getCustomGoalProcess().setGoal(new GoalXZ(x, z));
                CommandManager.sendMessage("§aGoal set: X=" + x + " Z=" + z);
            } else {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                b.getCustomGoalProcess().setGoal(new GoalBlock(x, y, z));
                CommandManager.sendMessage("§aGoal set: " + x + " " + y + " " + z);
            }
        } catch (NumberFormatException e) {
            CommandManager.sendMessage("§cUsage: .b goal [x] [y] [z]");
        }
    }

    private void handleSet(String[] args) {
        if (args.length < 2) {
            CommandManager.sendMessage("§cUsage: .b set <setting> <value>");
            return;
        }
        // Pass raw text to Baritone's command system
        handleRaw(new String[]{"set", args[0], args[1]});
    }

    /**
     * Pass raw text to Baritone's built-in command system (fallback).
     */
    private void handleRaw(String[] args) {
        try {
            String rawCommand = String.join(" ", args);
            IBaritone b = BaritoneAPI.getProvider().getPrimaryBaritone();
            b.getCommandManager().execute(rawCommand);
        } catch (Exception e) {
            CommandManager.sendMessage("§cBaritone error: " + e.getMessage());
        }
    }

    /* ── Help ─────────────────────────────────────────────── */

    private void showHelp() {
        CommandManager.sendMessage("§d§l═══ Baritone Commands ═══");
        CommandManager.sendMessage("§b.b goto §7<x> [y] <z> §8— path to coords");
        CommandManager.sendMessage("§b.b mine §7<block> [block2] §8— mine blocks");
        CommandManager.sendMessage("§b.b follow §7<player|all> §8— follow player");
        CommandManager.sendMessage("§b.b farm §7[radius] §8— auto-farm crops");
        CommandManager.sendMessage("§b.b explore §8— explore the world");
        CommandManager.sendMessage("§b.b elytra §7<x> <z> §8— elytra fly");
        CommandManager.sendMessage("§b.b thisway §7<dist> §8— go in look direction");
        CommandManager.sendMessage("§b.b goal §7[x] [y] [z] §8— set/view goal");
        CommandManager.sendMessage("§b.b stop §8— cancel all pathing");
        CommandManager.sendMessage("§b.b status §8— show status");
        CommandManager.sendMessage("§b.b set §7<key> <val> §8— change Baritone setting");
        CommandManager.sendMessage("§b.b §7<raw> §8— raw Baritone command");
    }

    /* ── Tab completion ───────────────────────────────────── */

    @Override
    public List<String> getCompletions(String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length <= 1) {
            String partial = args.length == 1 ? args[0].toLowerCase() : "";
            for (String cmd : SUB_COMMANDS) {
                if (cmd.startsWith(partial)) {
                    result.add(cmd);
                }
            }
        } else if (args[0].equalsIgnoreCase("mine") && args.length == 2) {
            // Complete block names
            String partial = args[1].toLowerCase();
            for (var block : Registries.BLOCK) {
                Identifier id = Registries.BLOCK.getId(block);
                String name = id.getPath();
                if (name.startsWith(partial)) {
                    result.add(name);
                    if (result.size() > 20) break;
                }
            }
        } else if (args[0].equalsIgnoreCase("follow") && args.length == 2) {
            String partial = args[1].toLowerCase();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) {
                result.add("all");
                for (var player : mc.world.getPlayers()) {
                    String name = player.getName().getString();
                    if (name.toLowerCase().startsWith(partial) && player != mc.player) {
                        result.add(name);
                    }
                }
            }
        }
        return result;
    }
}
