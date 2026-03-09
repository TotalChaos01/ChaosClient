package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.font.ChaosFont;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.Animate;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rise-style in-game HUD with multi-theme support.
 * Features: animated arraylist, themed watermark, BPS counter,
 * coordinates, FPS, notifications rendering.
 *
 * Ported from Rise Client's IngameGUI.java (774 lines).
 */
@ModuleInfo(name = "HUD", description = "In-game HUD overlay", category = Category.RENDER, hidden = true)
public class HUD extends Module {

    private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    private final NumberSetting arraySpacing = new NumberSetting("Array Spacing", 11, 8, 16, 1);
    private final NumberSetting arrayAnimSpeed = new NumberSetting("Array Anim Speed", 0.20, 0.05, 1.0, 0.05);
    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting coordinates = new BooleanSetting("Coordinates", true);
    private final BooleanSetting fps = new BooleanSetting("FPS", true);
    private final BooleanSetting bps = new BooleanSetting("BPS", false);
    private final BooleanSetting notifications = new BooleanSetting("Notifications", true);
    private final ModeSetting theme = new ModeSetting("Theme", "Chaos", ThemeUtil.getThemeNames());

    // Animation state — tracks each module's slide-in X position
    private final Map<Module, Float> moduleAnimX = new HashMap<>();
    private final Map<Module, String> displayNameCache = new HashMap<>();
    private final Map<Module, Integer> displayWidthCache = new HashMap<>();
    private List<Module> cachedArrayModules = List.of();
    private int cachedArrayHash = Integer.MIN_VALUE;

    public HUD() {
        addSettings(arrayList, arraySpacing, arrayAnimSpeed, watermark, coordinates, fps, bps, notifications, theme);
    }

    @Override
    public String getSuffix() {
        return theme.getMode();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen instanceof me.totalchaos01.chaosclient.ui.clickgui.ClickGuiScreen) return;
        if (mc.currentScreen != null && mc.currentScreen.shouldPause()
                && !(mc.currentScreen instanceof me.totalchaos01.chaosclient.ui.hud.HUDEditorScreen)) {
            return;
        }

        DrawContext ctx = event.getDrawContext();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        // Ensure HUD manager is initialized
        var mgr = ChaosClient.getInstance().getHudManager();
        if (mgr != null) mgr.init();

        // ─── Watermark (Rise-style, top-left) ─────────────────
        if (watermark.isEnabled()) {
            renderWatermark(ctx);
        }

        // ─── ArrayList (right side, Rise-style) ───────────────
        if (arrayList.isEnabled()) {
            renderArrayList(ctx, screenWidth);
        }

        // ─── BPS Counter ──────────────────────────────────────
        if (bps.isEnabled()) {
            float bpsX = 4, bpsY = screenHeight - 34;
            if (mgr != null) {
                var el = mgr.get("bps");
                if (el != null) { bpsX = el.getX(); bpsY = el.getY(); }
            }
            double dx = mc.player.getX() - mc.player.lastX;
            double dz = mc.player.getZ() - mc.player.lastZ;
            double speed = Math.sqrt(dx * dx + dz * dz) * 20; // blocks per second
            String bpsText = String.format("%.1f b/s", speed);
            ChaosFont.drawWithShadow(ctx, bpsText, (int) bpsX, (int) bpsY, 0xFFBBBBBB);
            // Update element size for editor
            if (mgr != null) {
                var el = mgr.get("bps");
                if (el != null) { el.setWidth(ChaosFont.getWidth(bpsText) + 4); el.setHeight(12); }
            }
        }

        // ─── Coordinates ──────────────────────────────────────
        if (coordinates.isEnabled()) {
            float coordX = 4, coordY = screenHeight - 14;
            if (mgr != null) {
                var el = mgr.get("coordinates");
                if (el != null) { coordX = el.getX(); coordY = el.getY(); }
            }
            String coords = String.format("§fXYZ: §b%.1f §f/ §b%.1f §f/ §b%.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ChaosFont.drawWithShadow(ctx, coords, (int) coordX, (int) coordY, 0xFFFFFFFF);
            if (mgr != null) {
                var el = mgr.get("coordinates");
                if (el != null) { el.setWidth(ChaosFont.getWidth(coords) + 4); el.setHeight(12); }
            }
        }

        // ─── FPS ──────────────────────────────────────────────
        if (fps.isEnabled()) {
            float fpsX = 4, fpsY = screenHeight - 24;
            if (mgr != null) {
                var el = mgr.get("fps");
                if (el != null) { fpsX = el.getX(); fpsY = el.getY(); }
            }
            String fpsText = "§fFPS: §a" + mc.getCurrentFps();
            ChaosFont.drawWithShadow(ctx, fpsText, (int) fpsX, (int) fpsY, 0xFFFFFFFF);
            if (mgr != null) {
                var el = mgr.get("fps");
                if (el != null) { el.setWidth(ChaosFont.getWidth(fpsText) + 4); el.setHeight(12); }
            }
        }

        // ─── Notifications ────────────────────────────────────
        if (notifications.isEnabled() && ChaosClient.getInstance().getNotificationManager() != null) {
            ChaosClient.getInstance().getNotificationManager().render(ctx);
        }
    }

    // ─── Watermark rendering ──────────────────────────────────

    private void renderWatermark(DrawContext ctx) {
        // HUD Editor position offset
        float offX = 0, offY = 0;
        try {
            var mgr = ChaosClient.getInstance().getHudManager();
            if (mgr != null) {
                mgr.init();
                var el = mgr.get("watermark");
                if (el != null) { offX = el.getX(); offY = el.getY(); }
            }
        } catch (Exception ignored) {}

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(offX, offY);

        String name = ChaosClient.CLIENT_NAME;
        String ver = "v" + ChaosClient.CLIENT_VERSION;
        Color themeColor = ThemeUtil.getThemeColor(0, ThemeType.LOGO, 1);
        int nameW = ChaosFont.getWidth(name);
        int verW = ChaosFont.getWidth(ver);
        int boxW = Math.max(nameW, verW) + 12;

        String themeMode = theme.getMode();

        switch (themeMode) {
            case "Skeet" -> {
                // Skeet — dark bordered bar, name + version on 2 lines
                ctx.fill(3, 3, 4 + boxW + 4, 28, 0xFF222222);
                ctx.fill(4, 4, 3 + boxW + 3, 27, 0xFF1A1A1A);
                ChaosFont.drawWithShadow(ctx, name, 6, 5, ColorUtil.toARGB(themeColor));
                ChaosFont.drawWithShadow(ctx, ver, 6, 16, 0xFF666666);
            }
            case "Never Lose" -> {
                // NeverLose — info bar with player name
                String user = mc.player != null ? mc.player.getName().getString() : "Player";
                RenderUtil.roundedRectSimple(ctx, 3, 3, boxW + 8, 26, 6, 0xCC1A1A2E);
                ChaosFont.drawWithShadow(ctx, name, 8, 5, ColorUtil.toARGB(themeColor));
                ChaosFont.drawWithShadow(ctx, "§7" + ver + " §8| §7" + user, 8, 16, 0xFF888888);
            }
            case "One Tap" -> {
                // OneTap — minimal transparent bar
                ctx.fill(3, 3, 4 + boxW + 4, 28, 0x60000000);
                ChaosFont.drawWithShadow(ctx, name, 6, 5, 0xFFFFFFFF);
                ChaosFont.drawWithShadow(ctx, ver, 6, 16, 0xFF999999);
            }
            default -> {
                // Rise-style — gradient text with glow, version below
                Color glowColor = ThemeUtil.getThemeColor(0, ThemeType.LOGO, 1);
                RenderUtil.glow(ctx, 2, 1, nameW + 6, 22, 5,
                        new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 25), 3);

                RenderUtil.drawGradientText(ctx, name, 4, 4, 0, 1.5f);
                ChaosFont.drawWithShadow(ctx, "§7" + ver, 4, 15, 0xFF888888);
            }
        }

        ctx.getMatrices().popMatrix();
    }

    // ─── ArrayList rendering ──────────────────────────────────

    private void renderArrayList(DrawContext ctx, int screenWidth) {
        int screenHeight = mc.getWindow().getScaledHeight();
        // Get position from HUD manager
        float baseX = screenWidth - 120, baseY = 2;
        var mgr = ChaosClient.getInstance().getHudManager();
        if (mgr != null) {
            var el = mgr.get("arraylist");
            if (el != null) { baseX = el.getX(); baseY = el.getY(); }
        }

        refreshArrayCache();

        float y = baseY;
        float maxWidth = 0;
        String themeMode = theme.getMode();
        int rowStep = (int) arraySpacing.getValue();

        for (int i = 0; i < cachedArrayModules.size(); i++) {
            Module m = cachedArrayModules.get(i);
            String displayName = displayNameCache.getOrDefault(m, getDisplayName(m));
            int textWidth = displayWidthCache.getOrDefault(m, ChaosFont.getWidth(displayName));
            maxWidth = Math.max(maxWidth, textWidth + 6);

            float elWidth = (mgr != null && mgr.get("arraylist") != null ? mgr.get("arraylist").getWidth() : 116);
            float rightEdge = baseX + elWidth;
            float targetX = rightEdge - textWidth - 4;

            // Animate X position (slide-in from right)
            float currentX = moduleAnimX.getOrDefault(m, targetX + 50);
            currentX = (float) Animate.lerp(currentX, targetX, (float) arrayAnimSpeed.getValue());
            moduleAnimX.put(m, currentX);

            int x = (int) currentX;

            // Theme color per-element
            Color elementColor = ThemeUtil.getThemeColor(i * 2f, ThemeType.ARRAYLIST, 1);
            int color = ColorUtil.toARGB(elementColor);

            switch (themeMode) {
                case "Chaos", "Prism", "Aqua Blend", "Sunset", "Crimson",
                     "Cotton Candy", "Ocean", "Inferno", "Ember", "Shadow",
                     "Neon", "Classic Revamp" -> {
                    // Rise-style — glow behind text, colored text, side gradient line
                    Color gc = new Color(elementColor.getRed(), elementColor.getGreen(), elementColor.getBlue(), 20);
                    RenderUtil.glow(ctx, x - 3, y - 2, textWidth + 6, 12, 3, gc, 3);
                    RenderUtil.roundedRectGradientV(ctx, (int) rightEdge - 2, (int) y - 1, 2, 11, 1, color,
                            ColorUtil.toARGB(ThemeUtil.getThemeColor((i + 1) * 2f, ThemeType.ARRAYLIST, 1)));
                    ChaosFont.drawWithShadow(ctx, displayName, x, (int) y, color);
                }
                case "Comfort", "Comfort Rainbow" -> {
                    ctx.fill(x - 2, (int) y - 1, (int) rightEdge, (int) y + 10, 0x80000000);
                    ctx.fill((int) rightEdge - 2, (int) y - 1, (int) rightEdge, (int) y + 10, color);
                    ChaosFont.drawWithShadow(ctx, displayName, x, (int) y, color);
                }
                case "Skeet" -> {
                    ctx.fill(x - 3, (int) y - 1, (int) rightEdge, (int) y + 10, 0xE0111111);
                    ctx.fill((int) rightEdge - 2, (int) y - 1, (int) rightEdge, (int) y + 10, color);
                    ChaosFont.drawWithShadow(ctx, displayName, x, (int) y, 0xFFFFFFFF);
                }
                case "Never Lose" -> {
                    ctx.fill(x - 3, (int) y - 1, (int) rightEdge, (int) y + 10, 0x901A1A2E);
                    ctx.fill((int) rightEdge - 2, (int) y - 1, (int) rightEdge, (int) y + 10, color);
                    ChaosFont.drawWithShadow(ctx, displayName, x, (int) y, 0xFFDDDDDD);
                }
                case "One Tap" -> {
                    ctx.fill(x - 2, (int) y - 1, (int) rightEdge, (int) y + 10, 0x50000000);
                    ChaosFont.drawWithShadow(ctx, displayName, x, (int) y, 0xFFFFFFFF);
                }
                case "Minecraft", "Minecraft Rainbow" -> {
                    ctx.fill(x - 2, (int) y - 1, (int) rightEdge, (int) y + 10, 0x80000000);
                    ChaosFont.drawWithShadow(ctx, displayName, x, (int) y, color);
                }
                default -> {
                    ctx.fill((int) rightEdge - 2, (int) y - 1, (int) rightEdge, (int) y + 10, color);
                    ChaosFont.drawWithShadow(ctx, displayName, x, (int) y, color);
                }
            }

            y += rowStep;
        }

        // Update arraylist element size for HUD editor
        if (mgr != null) {
            var el = mgr.get("arraylist");
            if (el != null) {
                el.setHeight(Math.max(20, y - baseY));
                if (maxWidth > 0) el.setWidth(maxWidth + 8);
            }
        }

        // Clean up animation state for disabled modules
        moduleAnimX.keySet().removeIf(m -> !m.isEnabled() || m.isHidden());
    }

    private void refreshArrayCache() {
        int hash = 1;
        for (Module module : ChaosClient.getInstance().getModuleManager().getModules()) {
            hash = 31 * hash + module.getName().hashCode();
            hash = 31 * hash + (module.isEnabled() ? 1 : 0);
            hash = 31 * hash + (module.isHidden() ? 1 : 0);
            String suffix = module.getSuffix();
            if (suffix != null) hash = 31 * hash + suffix.hashCode();
        }
        if (hash == cachedArrayHash) return;

        cachedArrayHash = hash;
        cachedArrayModules = ChaosClient.getInstance().getModuleManager().getModules().stream()
                .filter(Module::isEnabled)
                .filter(m -> !m.isHidden())
                .sorted(Comparator.comparingInt((Module m) -> {
                    String name = getDisplayName(m);
                    displayNameCache.put(m, name);
                    int width = ChaosFont.getWidth(name);
                    displayWidthCache.put(m, width);
                    return width;
                }).reversed())
                .toList();

        displayNameCache.keySet().removeIf(m -> !cachedArrayModules.contains(m));
        displayWidthCache.keySet().removeIf(m -> !cachedArrayModules.contains(m));
    }

    private String getDisplayName(Module m) {
        String suffix = m.getSuffix();
        return suffix != null ? m.getName() + " §7" + suffix : m.getName();
    }
}
