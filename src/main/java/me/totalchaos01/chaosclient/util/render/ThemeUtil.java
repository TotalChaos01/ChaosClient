package me.totalchaos01.chaosclient.util.render;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.setting.Setting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;

import java.awt.*;

/**
 * Dynamic theme color engine — returns animated colors based on the current theme.
 * ChaosClient theme system with 19 unique themes.
 */
public final class ThemeUtil {

    private static String currentTheme = "Chaos";
    private static Color baseColor = new Color(147, 51, 234); // Purple — default ChaosClient color
    private static long lastUpdate = 0;

    // Preset blend pair
    private static final Color BLEND_C1 = new Color(71, 148, 253);
    private static final Color BLEND_C2 = new Color(71, 253, 160);

    private ThemeUtil() {}

    /**
     * Get the current theme name. Reads from HUD module "Theme" setting if available.
     */
    public static String getTheme() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 250) {
            lastUpdate = now;
            try {
                if (ChaosClient.getInstance() != null && ChaosClient.getInstance().getModuleManager() != null) {
                    Setting setting = ChaosClient.getInstance().getModuleManager().getSetting("HUD", "Theme");
                    if (setting instanceof ModeSetting ms) {
                        currentTheme = ms.getMode();
                    }
                }
            } catch (Exception ignored) {}
        }
        return currentTheme;
    }

    public static void setTheme(String theme) {
        currentTheme = theme;
        lastUpdate = System.currentTimeMillis();
        try {
            if (ChaosClient.getInstance() != null && ChaosClient.getInstance().getModuleManager() != null) {
                Setting setting = ChaosClient.getInstance().getModuleManager().getSetting("HUD", "Theme");
                if (setting instanceof ModeSetting ms) {
                    ms.setMode(theme);
                }
            }
        } catch (Exception ignored) {}
    }

    // ─── Primary API ──────────────────────────────────────────

    public static Color getThemeColor(ThemeType type) {
        return getThemeColor(0, type, 1);
    }

    public static int getThemeColorInt(ThemeType type) {
        return ColorUtil.toARGB(getThemeColor(type));
    }

    public static int getThemeColorInt(float colorOffset, ThemeType type) {
        return ColorUtil.toARGB(getThemeColor(colorOffset, type, 1));
    }

    public static Color getThemeColor(float colorOffset, ThemeType type) {
        return getThemeColor(colorOffset, type, 1);
    }

    /**
     * Core method — returns a Color for the given offset, type, and time multiplier.
     */
    public static Color getThemeColor(float colorOffset, ThemeType type, float timeMultiplier) {
        String theme = getTheme();

        float offsetMultiplier = 1;

        if (type == ThemeType.GENERAL || type == ThemeType.ARRAYLIST) {
            switch (theme) {
                case "Chaos", "Skeet", "Comfort", "Minecraft", "Never Lose" -> offsetMultiplier = 2.2f;
                case "Prism", "Comfort Rainbow", "Minecraft Rainbow" -> offsetMultiplier = 5f;
                case "Aqua Blend", "Crimson", "Cotton Candy", "Neon" -> offsetMultiplier = 2.5f;
            }
        }

        colorOffset *= offsetMultiplier;

        double timer = (System.currentTimeMillis() / 1E+8 * timeMultiplier) * 4E+5;
        double factor = (Math.sin(timer + colorOffset * 0.55f) + 1) * 0.5f;

        return switch (type) {
            case GENERAL, ARRAYLIST -> computeArraylistColor(theme, colorOffset, timer, factor);
            case LOGO -> computeLogoColor(theme, colorOffset, timer, factor);
            case FLAT_COLOR -> baseColor;
        };
    }

    private static Color computeArraylistColor(String theme, float offset, double timer, double factor) {
        return switch (theme) {
            case "Chaos", "Skeet", "Comfort", "Minecraft", "Never Lose" -> {
                float off = (float) (Math.abs(Math.sin(timer + offset * 0.45)) / 2) + 1;
                yield ColorUtil.brighter(baseColor, Math.max(0.01f, Math.min(0.99f, 1.0f / off)));
            }
            case "Prism", "Comfort Rainbow", "Minecraft Rainbow" ->
                    new Color(ColorUtil.getColor(-(1 + offset * 1.7f), 0.7f, 1));
            case "Classic Revamp" ->
                    new Color(ColorUtil.getColor(1 + offset * 1.4f, 0.6f, 1));
            case "Aqua Blend" -> ColorUtil.mixColors(BLEND_C1, BLEND_C2, factor);
            case "Crimson" -> ColorUtil.mixColors(Color.WHITE, Color.RED, factor);
            case "Cotton Candy" ->
                    ColorUtil.mixColors(new Color(255, 104, 204), new Color(99, 249, 255), factor);
            case "Sunset" ->
                    ColorUtil.mixColors(new Color(222, 90, 0), new Color(255, 0, 135), factor);
            case "Shadow" -> ColorUtil.mixColors(Color.DARK_GRAY, Color.WHITE, factor);
            case "Inferno" ->
                    ColorUtil.mixColors(new Color(255, 64, 5), new Color(219, 0, 220), factor);
            case "Ocean" ->
                    ColorUtil.mixColors(new Color(4, 0, 187), new Color(124, 243, 255), factor);
            case "Ember" -> ColorUtil.mixColors(Color.RED, Color.ORANGE, factor);
            case "Neon" ->
                    ColorUtil.mixColors(new Color(190, 0, 255), new Color(0, 190, 255), factor);
            case "One Tap" -> Color.WHITE;
            default -> baseColor;
        };
    }

    private static Color computeLogoColor(String theme, float offset, double timer, double factor) {
        return switch (theme) {
            case "Chaos", "Comfort", "Minecraft" -> baseColor;
            case "Prism", "Minecraft Rainbow" ->
                    new Color(ColorUtil.getColor(1 + offset * 1.4f, 0.5f, 1));
            case "Classic Revamp" ->
                    new Color(ColorUtil.getColor(1 + offset * 1.4f, 0.6f, 1));
            case "Shadow" -> ColorUtil.mixColors(Color.DARK_GRAY, Color.WHITE, factor);
            case "Crimson" -> ColorUtil.mixColors(Color.WHITE, Color.RED, factor);
            case "Cotton Candy" ->
                    ColorUtil.mixColors(new Color(255, 104, 204), new Color(99, 249, 255), factor);
            case "Ember" -> ColorUtil.mixColors(Color.RED, Color.ORANGE, factor);
            case "Sunset" ->
                    ColorUtil.mixColors(new Color(222, 90, 0), new Color(255, 0, 135), factor);
            case "Aqua Blend" -> ColorUtil.mixColors(BLEND_C1, BLEND_C2, factor);
            case "Ocean" ->
                    ColorUtil.mixColors(new Color(4, 0, 187), new Color(124, 243, 255), factor);
            case "Inferno" ->
                    ColorUtil.mixColors(new Color(255, 64, 5), new Color(219, 0, 220), factor);
            case "Neon" ->
                    ColorUtil.mixColors(new Color(190, 0, 255), new Color(0, 190, 255), factor);
            case "One Tap" -> Color.WHITE;
            default -> baseColor;
        };
    }

    // ─── Convenience ──────────────────────────────────────────

    public static Color getBaseColor() {
        return baseColor;
    }

    public static void setBaseColor(Color color) {
        baseColor = color;
    }

    /**
     * Get all available theme names.
     */
    public static String[] getThemeNames() {
        return new String[]{
                "Chaos", "Aqua Blend", "Prism", "Sunset",
                "Crimson", "Cotton Candy", "Ocean",
                "Inferno", "Ember", "Shadow",
                "Neon", "Comfort", "Comfort Rainbow",
                "Minecraft", "Minecraft Rainbow",
                "Skeet", "Never Lose", "One Tap", "Classic Revamp"
        };
    }
}
