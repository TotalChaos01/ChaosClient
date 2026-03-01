package me.totalchaos01.chaosclient.command.impl;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.command.CommandManager;
import me.totalchaos01.chaosclient.module.Module;

public class ToggleCommand extends Command {

    public ToggleCommand() {
        super("toggle", "Toggle a module on/off", ".toggle <module>", "t");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            CommandManager.sendMessage("§cUsage: " + getUsage());
            return;
        }

        Module module = ChaosClient.getInstance().getModuleManager().getModule(args[0]);
        if (module == null) {
            CommandManager.sendMessage("§cModule '" + args[0] + "' not found!");
            return;
        }

        module.toggle();
        CommandManager.sendMessage("§f" + module.getName() + " §7is now " +
                (module.isEnabled() ? "§aenabled" : "§cdisabled"));
    }
}
