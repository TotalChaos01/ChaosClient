package me.totalchaos01.chaosclient.command.impl;

import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.command.CommandManager;

import java.util.ArrayList;
import java.util.List;

public class FriendCommand extends Command {

    public static final List<String> friends = new ArrayList<>();

    public FriendCommand() {
        super("friend", "Manage friend list", ".friend add/remove/list <name>", "f");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.sendMessage("§cUsage: " + getUsage());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    CommandManager.sendMessage("§cUsage: .friend add <name>");
                    return;
                }
                if (friends.contains(args[1].toLowerCase())) {
                    CommandManager.sendMessage("§c" + args[1] + " is already a friend!");
                    return;
                }
                friends.add(args[1].toLowerCase());
                CommandManager.sendMessage("§aAdded §f" + args[1] + " §ato friends!");
            }
            case "remove", "del" -> {
                if (args.length < 2) {
                    CommandManager.sendMessage("§cUsage: .friend remove <name>");
                    return;
                }
                if (!friends.remove(args[1].toLowerCase())) {
                    CommandManager.sendMessage("§c" + args[1] + " is not a friend!");
                    return;
                }
                CommandManager.sendMessage("§cRemoved §f" + args[1] + " §cfrom friends!");
            }
            case "list" -> {
                if (friends.isEmpty()) {
                    CommandManager.sendMessage("§7No friends added.");
                } else {
                    CommandManager.sendMessage("§f--- §dFriends §f---");
                    friends.forEach(f -> CommandManager.sendMessage("§b" + f));
                }
            }
            default -> CommandManager.sendMessage("§cUsage: .friend add/remove/list <name>");
        }
    }

    public static boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }
}
