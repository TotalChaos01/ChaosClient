package me.totalchaos01.chaosclient.command.impl;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.command.CommandManager;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Save or load configurations", ".config save/load", "cfg");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.sendMessage("§cUsage: " + getUsage());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "save" -> {
                ChaosClient.getInstance().getConfigManager().save();
                CommandManager.sendMessage("§aConfig saved!");
            }
            case "load" -> {
                ChaosClient.getInstance().getConfigManager().load();
                CommandManager.sendMessage("§aConfig loaded!");
            }
            default -> CommandManager.sendMessage("§cUsage: " + getUsage());
        }
    }
}
