package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;

import java.util.Set;

/**
 * XRAY — makes non-ore blocks transparent to see ores through walls.
 * Requires a mixin to modify block rendering to make blocks invisible.
 */
@ModuleInfo(name = "XRAY", description = "See ores through walls", category = Category.RENDER)
public class XRAY extends Module {

    private final BooleanSetting diamonds = new BooleanSetting("Diamonds", true);
    private final BooleanSetting iron = new BooleanSetting("Iron", true);
    private final BooleanSetting gold = new BooleanSetting("Gold", true);
    private final BooleanSetting emeralds = new BooleanSetting("Emeralds", true);
    private final BooleanSetting redstone = new BooleanSetting("Redstone", true);
    private final BooleanSetting lapis = new BooleanSetting("Lapis", true);
    private final BooleanSetting coal = new BooleanSetting("Coal", false);
    private final BooleanSetting copper = new BooleanSetting("Copper", false);
    private final BooleanSetting netherite = new BooleanSetting("Ancient Debris", true);
    private final BooleanSetting quartz = new BooleanSetting("Nether Quartz", false);

    public XRAY() {
        addSettings(diamonds, iron, gold, emeralds, redstone, lapis, coal, copper, netherite, quartz);
    }

    @Override
    protected void onEnable() {
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    protected void onDisable() {
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    /**
     * Checks if a given block should be visible in X-Ray mode.
     */
    public boolean isVisibleBlock(net.minecraft.block.Block block) {
        if (!isEnabled()) return true;

        String blockName = block.getTranslationKey().toLowerCase();

        if (diamonds.isEnabled() && blockName.contains("diamond_ore")) return true;
        if (iron.isEnabled() && blockName.contains("iron_ore")) return true;
        if (gold.isEnabled() && blockName.contains("gold_ore")) return true;
        if (emeralds.isEnabled() && blockName.contains("emerald_ore")) return true;
        if (redstone.isEnabled() && blockName.contains("redstone_ore")) return true;
        if (lapis.isEnabled() && blockName.contains("lapis_ore")) return true;
        if (coal.isEnabled() && blockName.contains("coal_ore")) return true;
        if (copper.isEnabled() && blockName.contains("copper_ore")) return true;
        if (netherite.isEnabled() && blockName.contains("ancient_debris")) return true;
        if (quartz.isEnabled() && blockName.contains("nether_quartz_ore")) return true;

        // Always show bedrock, obsidian, and some utility blocks
        if (blockName.contains("chest") || blockName.contains("spawner") ||
            blockName.contains("portal") || blockName.contains("bedrock")) return true;

        return false;
    }
}
