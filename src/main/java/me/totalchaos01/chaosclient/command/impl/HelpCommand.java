package me.totalchaos01.chaosclient.command.impl;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.command.CommandManager;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Shows all available commands", ".help", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        CommandManager.sendMessage("§f--- §dChaosClient Commands §f---");
        for (Command cmd : ChaosClient.getInstance().getCommandManager().getCommands()) {
            CommandManager.sendMessage("§b." + cmd.getName() + " §7- " + cmd.getDescription());
        }
    }
}
