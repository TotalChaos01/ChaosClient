package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
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
@ModuleInfo(name = "HUD", description = "Rise-style HUD overlay", category = Category.RENDER, hidden = true)
public class HUD extends Module {

    private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting coordinates = new BooleanSetting("Coordinates", true);
    private final BooleanSetting fps = new BooleanSetting("FPS", true);
    private final BooleanSetting bps = new BooleanSetting("BPS", false);
    private final BooleanSetting notifications = new BooleanSetting("Notifications", true);
    private final ModeSetting theme = new ModeSetting("Theme", "Rise", ThemeUtil.getThemeNames());

    // Animation state — tracks each module's slide-in X position
    private final Map<Module, Float> moduleAnimX = new HashMap<>();

    public HUD() {
        addSettings(arrayList, watermark, coordinates, fps, bps, notifications, theme);
    }

    @Override
    public String getSuffix() {
        return theme.getMode();
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;

        DrawContext ctx = event.getDrawContext();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

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
            double dx = mc.player.getX() - mc.player.lastX;
            double dz = mc.player.getZ() - mc.player.lastZ;
            double speed = Math.sqrt(dx * dx + dz * dz) * 20; // blocks per second
            String bpsText = String.format("%.1f b/s", speed);
            ctx.drawTextWithShadow(mc.textRenderer, bpsText, 4, screenHeight - 34, 0xFFBBBBBB);
        }

        // ─── Coordinates ──────────────────────────────────────
        if (coordinates.isEnabled()) {
            String coords = String.format("§fXYZ: §b%.1f §f/ §b%.1f §f/ §b%.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawTextWithShadow(mc.textRenderer, coords, 4, screenHeight - 14, 0xFFFFFFFF);
        }

        // ─── FPS ──────────────────────────────────────────────
        if (fps.isEnabled()) {
            String fpsText = "§fFPS: §a" + mc.getCurrentFps();
            ctx.drawTextWithShadow(mc.textRenderer, fpsText, 4, screenHeight - 24, 0xFFFFFFFF);
        }

        // ─── Notifications ────────────────────────────────────
        if (notifications.isEnabled() && ChaosClient.getInstance().getNotificationManager() != null) {
            ChaosClient.getInstance().getNotificationManager().render(ctx);
        }
    }

    // ─── Watermark rendering ──────────────────────────────────

    private void renderWatermark(DrawContext ctx) {
        String name = ChaosClient.CLIENT_NAME;
        String ver = "v" + ChaosClient.CLIENT_VERSION;
        Color themeColor = ThemeUtil.getThemeColor(0, ThemeType.LOGO, 1);
        int nameW = mc.textRenderer.getWidth(name);
        int verW = mc.textRenderer.getWidth(ver);
        int boxW = Math.max(nameW, verW) + 12;

        String themeMode = theme.getMode();

        switch (themeMode) {
            case "Skeet" -> {
                // Skeet — dark bordered bar, name + version on 2 lines
                ctx.fill(3, 3, 4 + boxW + 4, 28, 0xFF222222);
                ctx.fill(4, 4, 3 + boxW + 3, 27, 0xFF1A1A1A);
                ctx.drawTextWithShadow(mc.textRenderer, name, 6, 5, ColorUtil.toARGB(themeColor));
                ctx.drawTextWithShadow(mc.textRenderer, ver, 6, 16, 0xFF666666);
            }
            case "Never Lose" -> {
                // NeverLose — info bar with player name
                String user = mc.player != null ? mc.player.getName().getString() : "Player";
                RenderUtil.roundedRectSimple(ctx, 3, 3, boxW + 8, 26, 6, 0xCC1A1A2E);
                ctx.drawTextWithShadow(mc.textRenderer, name, 8, 5, ColorUtil.toARGB(themeColor));
                ctx.drawTextWithShadow(mc.textRenderer, "§7" + ver + " §8| §7" + user, 8, 16, 0xFF888888);
            }
            case "One Tap" -> {
                // OneTap — minimal transparent bar
                ctx.fill(3, 3, 4 + boxW + 4, 28, 0x60000000);
                ctx.drawTextWithShadow(mc.textRenderer, name, 6, 5, 0xFFFFFFFF);
                ctx.drawTextWithShadow(mc.textRenderer, ver, 6, 16, 0xFF999999);
            }
            default -> {
                // Rise-style — gradient text with glow, version below
                Color glowColor = ThemeUtil.getThemeColor(0, ThemeType.LOGO, 1);
                RenderUtil.glow(ctx, 2, 1, nameW + 6, 22, 5,
                        new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 25), 3);

                RenderUtil.drawGradientText(ctx, name, 4, 4, 0, 1.5f);
                ctx.drawTextWithShadow(mc.textRenderer, "§7" + ver, 4, 15, 0xFF888888);
            }
        }
    }

    // ─── ArrayList rendering ──────────────────────────────────

    private void renderArrayList(DrawContext ctx, int screenWidth) {
        List<Module> enabledModules = ChaosClient.getInstance().getModuleManager().getModules().stream()
                .filter(Module::isEnabled)
                .filter(m -> !m.isHidden())
                .sorted(Comparator.comparingInt((Module m) -> mc.textRenderer.getWidth(getDisplayName(m))).reversed())
                .toList();

        int y = 2;
        String themeMode = theme.getMode();

        for (int i = 0; i < enabledModules.size(); i++) {
            Module m = enabledModules.get(i);
            String displayName = getDisplayName(m);
            int textWidth = mc.textRenderer.getWidth(displayName);

            // Animate X position (slide-in from right)
            float targetX = screenWidth - textWidth - 4;
            float currentX = moduleAnimX.getOrDefault(m, (float) screenWidth);
            currentX = (float) Animate.lerp(currentX, targetX, 0.2);
            moduleAnimX.put(m, currentX);

            int x = (int) currentX;

            // Theme color per-element
            Color elementColor = ThemeUtil.getThemeColor(i * 2f, ThemeType.ARRAYLIST, 1);
            int color = ColorUtil.toARGB(elementColor);

            switch (themeMode) {
                case "Rise", "Rise Rainbow", "Rise Blend", "Rise 6 Old", "Rise Christmas",
                     "Rise Cotton Candy", "Rise Sea", "Rise Cool", "Rise Blaze", "Rise Emo",
                     "Rice", "Classic Revamp" -> {
                    // Rise-style — glow behind text, colored text, side gradient line
                    Color gc = new Color(elementColor.getRed(), elementColor.getGreen(), elementColor.getBlue(), 20);
                    RenderUtil.glow(ctx, x - 3, y - 2, textWidth + 6, 12, 3, gc, 3);
                    RenderUtil.roundedRectGradientV(ctx, screenWidth - 2, y - 1, 2, 11, 1, color,
                            ColorUtil.toARGB(ThemeUtil.getThemeColor((i + 1) * 2f, ThemeType.ARRAYLIST, 1)));
                    ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, color);
                }
                case "Comfort", "Comfort Rainbow" -> {
                    // Comfort — with background rect
                    ctx.fill(x - 2, y - 1, screenWidth, y + 10, 0x80000000);
                    ctx.fill(screenWidth - 2, y - 1, screenWidth, y + 10, color);
                    ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, color);
                }
                case "Skeet" -> {
                    // Skeet — dark background, white text with side bar
                    ctx.fill(x - 3, y - 1, screenWidth, y + 10, 0xE0111111);
                    ctx.fill(screenWidth - 2, y - 1, screenWidth, y + 10, color);
                    ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, 0xFFFFFFFF);
                }
                case "Never Lose" -> {
                    // NeverLose — semi-transparent bg with accent line
                    ctx.fill(x - 3, y - 1, screenWidth, y + 10, 0x901A1A2E);
                    ctx.fill(screenWidth - 2, y - 1, screenWidth, y + 10, color);
                    ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, 0xFFDDDDDD);
                }
                case "One Tap" -> {
                    // OneTap — minimal, white text
                    ctx.fill(x - 2, y - 1, screenWidth, y + 10, 0x50000000);
                    ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, 0xFFFFFFFF);
                }
                case "Minecraft", "Minecraft Rainbow" -> {
                    // Minecraft — vanilla-like colored text
                    ctx.fill(x - 2, y - 1, screenWidth, y + 10, 0x80000000);
                    ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, color);
                }
                default -> {
                    ctx.fill(screenWidth - 2, y - 1, screenWidth, y + 10, color);
                    ctx.drawTextWithShadow(mc.textRenderer, displayName, x, y, color);
                }
            }

            y += 11;
        }

        // Clean up animation state for disabled modules
        moduleAnimX.keySet().removeIf(m -> !m.isEnabled() || m.isHidden());
    }

    private String getDisplayName(Module m) {
        String suffix = m.getSuffix();
        return suffix != null ? m.getName() + " §7" + suffix : m.getName();
    }
}
