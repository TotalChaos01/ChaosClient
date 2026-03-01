package me.totalchaos01.chaosclient.command.impl;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.command.CommandManager;
import me.totalchaos01.chaosclient.module.Module;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Bind a module to a key", ".bind <module> <key>", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CommandManager.sendMessage("§cUsage: " + getUsage());
            return;
        }

        Module module = ChaosClient.getInstance().getModuleManager().getModule(args[0]);
        if (module == null) {
            CommandManager.sendMessage("§cModule '" + args[0] + "' not found!");
            return;
        }

        String keyName = args[1].toUpperCase();
        int keyCode = getKeyCode(keyName);
        if (keyCode == -1) {
            CommandManager.sendMessage("§cUnknown key: " + args[1]);
            return;
        }

        module.setKeyBind(keyCode);
        CommandManager.sendMessage("§f" + module.getName() + " §7bound to §b" + keyName);
    }

    private int getKeyCode(String name) {
        try {
            Field field = GLFW.class.getField("GLFW_KEY_" + name);
            return field.getInt(null);
        } catch (Exception e) {
            return -1;
        }
    }
}
