package me.totalchaos01.chaosclient.command.impl;

import me.totalchaos01.chaosclient.command.Command;
import me.totalchaos01.chaosclient.command.CommandManager;
import net.minecraft.client.MinecraftClient;

/**
 * Vertical clip — teleports the player up or down by a specified distance.
 * Usage: .vclip <distance>  (positive = up, negative = down)
 */
public class VClipCommand extends Command {

    public VClipCommand() {
        super("vclip", "Teleport vertically", ".vclip <distance>", "vc");
    }

    @Override
    public void execute(String[] args) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (args.length < 1) {
            CommandManager.sendMessage("§cUsage: " + getUsage());
            return;
        }

        try {
            double distance = Double.parseDouble(args[0]);
            mc.player.setPosition(mc.player.getX(), mc.player.getY() + distance, mc.player.getZ());
            CommandManager.sendMessage("§aTeleported " + (distance > 0 ? "up" : "down") + " " +
                    String.format("%.1f", Math.abs(distance)) + " blocks");
        } catch (NumberFormatException e) {
            CommandManager.sendMessage("§cInvalid number: " + args[0]);
        }
    }
}
