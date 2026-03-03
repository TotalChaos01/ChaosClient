package me.totalchaos01.chaosclient.command;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.command.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages all chat commands (prefix: .)
 */
public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public void init() {
        commands.add(new HelpCommand());
        commands.add(new ToggleCommand());
        commands.add(new BindCommand());
        commands.add(new FriendCommand());
        commands.add(new ConfigCommand());
        commands.add(new VClipCommand());
        commands.add(new BaritoneCommand());
    }

    public void handle(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return;

        String name = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        for (Command cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(name) || Arrays.asList(cmd.getAliases()).contains(name)) {
                try {
                    cmd.execute(args);
                } catch (Exception e) {
                    sendMessage("§cError: " + e.getMessage());
                }
                return;
            }
        }

        sendMessage("§cUnknown command. Type .help for a list of commands.");
    }

    public static void sendMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§d" + ChaosClient.CLIENT_NAME + " §7» " + message), false);
        }
    }

    public List<Command> getCommands() {
        return commands;
    }

    /**
     * Returns tab-completions for the given partial command input (after the dot prefix).
     * Used by the MixinChatScreen for TAB completion of .commands.
     */
    public List<String> getCompletions(String input) {
        String[] parts = input.split("\\s+", -1);
        List<String> result = new java.util.ArrayList<>();

        if (parts.length <= 1) {
            // Complete command names
            String partial = (parts.length > 0 ? parts[0] : "").toLowerCase();
            for (Command cmd : commands) {
                if (cmd.getName().toLowerCase().startsWith(partial)) {
                    result.add(cmd.getName());
                }
                for (String alias : cmd.getAliases()) {
                    if (alias.toLowerCase().startsWith(partial)) {
                        result.add(alias);
                    }
                }
            }
        } else {
            String cmdName = parts[0].toLowerCase();
            String partial = parts[parts.length - 1].toLowerCase();

            // Module name completions for toggle/bind
            if ("toggle".equals(cmdName) || "t".equals(cmdName) || "bind".equals(cmdName) || "b".equals(cmdName)) {
                if (ChaosClient.getInstance() != null && ChaosClient.getInstance().getModuleManager() != null) {
                    for (var mod : ChaosClient.getInstance().getModuleManager().getModules()) {
                        if (mod.getName().toLowerCase().startsWith(partial)) {
                            result.add(parts[0] + " " + mod.getName());
                        }
                    }
                }
            }
            // Subcommand completions
            else if ("config".equals(cmdName) || "cfg".equals(cmdName)) {
                for (String s : new String[]{"save", "load"}) {
                    if (s.startsWith(partial)) result.add(parts[0] + " " + s);
                }
            }
            else if ("friend".equals(cmdName) || "f".equals(cmdName)) {
                for (String s : new String[]{"add", "remove", "list"}) {
                    if (s.startsWith(partial)) result.add(parts[0] + " " + s);
                }
            }
        }
        return result;
    }
}
