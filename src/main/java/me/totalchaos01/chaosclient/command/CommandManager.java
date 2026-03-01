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
}
