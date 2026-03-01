package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.List;

/**
 * Renders the in-game HUD overlay with module list (array list) and client info.
 */
@ModuleInfo(name = "HUD", description = "Shows client HUD overlay", category = Category.RENDER, hidden = true)
public class HUD extends Module {

    private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting coordinates = new BooleanSetting("Coordinates", true);
    private final BooleanSetting fps = new BooleanSetting("FPS", true);

    public HUD() {
        addSettings(arrayList, watermark, coordinates, fps);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;

        DrawContext ctx = event.getDrawContext();
        int screenWidth = mc.getWindow().getScaledWidth();

        // Watermark
        if (watermark.isEnabled()) {
            String text = ChaosClient.CLIENT_NAME + " §7v" + ChaosClient.CLIENT_VERSION;
            ctx.drawTextWithShadow(mc.textRenderer, text, 4, 4, 0xFFAA55FF);
        }

        // ArrayList — enabled modules sorted by name length
        if (arrayList.isEnabled()) {
            List<Module> enabledModules = ChaosClient.getInstance().getModuleManager().getModules().stream()
                    .filter(Module::isEnabled)
                    .filter(m -> !m.isHidden())
                    .sorted(Comparator.comparingInt((Module m) -> mc.textRenderer.getWidth(getDisplayName(m))).reversed())
                    .toList();

            int y = 2;
            for (int i = 0; i < enabledModules.size(); i++) {
                Module m = enabledModules.get(i);
                String displayName = getDisplayName(m);
                int textWidth = mc.textRenderer.getWidth(displayName);
                int x = screenWidth - textWidth - 4;

                // Background
                ctx.fill(x - 2, y - 1, screenWidth, y + 10, 0x80000000);

                // Color gradient effect (purple to blue)
                float hue = (float) i / Math.max(enabledModules.size(), 1) * 0.3f + 0.75f;
                int color = java.awt.Color.HSBtoRGB(hue, 0.7f, 1.0f);

                // Side line
                ctx.fill(screenWidth - 1, y - 1, screenWidth, y + 10, color);

                ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, color);
                y += 11;
            }
        }

        // Coordinates
        if (coordinates.isEnabled()) {
            int screenHeight = mc.getWindow().getScaledHeight();
            String coords = String.format("§fXYZ: §b%.1f §f/ §b%.1f §f/ §b%.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawTextWithShadow(mc.textRenderer, coords, 4, screenHeight - 14, 0xFFFFFFFF);
        }

        // FPS
        if (fps.isEnabled()) {
            int screenHeight = mc.getWindow().getScaledHeight();
            String fpsText = "§fFPS: §a" + mc.getCurrentFps();
            ctx.drawTextWithShadow(mc.textRenderer, fpsText, 4, screenHeight - 24, 0xFFFFFFFF);
        }
    }

    private String getDisplayName(Module m) {
        String suffix = m.getSuffix();
        return suffix != null ? m.getName() + " §7" + suffix : m.getName();
    }
}
