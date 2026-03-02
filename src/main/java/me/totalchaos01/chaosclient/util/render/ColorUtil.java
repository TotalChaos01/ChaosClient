package me.totalchaos01.chaosclient.util.render;

import java.awt.*;
import java.util.regex.Pattern;

/**
 * Color manipulation utilities.
 * Ported from Rise Client — pure Java, no MC dependencies.
 */
public final class ColorUtil {

    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    private ColorUtil() {}

    // ─── Brightness ───────────────────────────────────────────

    public static Color brighter(Color c, float factor) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        int i = (int) (1.0 / (1.0 - factor));
        if (r == 0 && g == 0 && b == 0) return new Color(i, i, i, a);
        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;
        return new Color(
                Math.min((int) (r / factor), 255),
                Math.min((int) (g / factor), 255),
                Math.min((int) (b / factor), 255), a
        );
    }

    public static Color darker(Color c, double factor) {
        return new Color(
                Math.max((int) (c.getRed() * factor), 0),
                Math.max((int) (c.getGreen() * factor), 0),
                Math.max((int) (c.getBlue() * factor), 0),
                c.getAlpha()
        );
    }

    // ─── Rainbow / Animated Colors ────────────────────────────

    public static int getColor(float hueOffset, float saturation, float brightness) {
        float speed = 4500;
        float hue = (System.currentTimeMillis() % (int) speed) / speed;
        return Color.HSBtoRGB(hue - hueOffset / 54, saturation, brightness);
    }

    public static int getStaticColor(float hueOffset, float saturation, float brightness) {
        return Color.HSBtoRGB(hueOffset / 54, saturation, brightness);
    }

    public static int getRainbow() {
        float hue = (System.currentTimeMillis() % 10000) / 10000f;
        return Color.HSBtoRGB(hue, 0.5f, 1);
    }

    // ─── Mixing / Blending ────────────────────────────────────

    public static Color mixColors(Color color1, Color color2, double percent) {
        double inv = 1.0 - percent;
        int r = (int) (color1.getRed() * percent + color2.getRed() * inv);
        int g = (int) (color1.getGreen() * percent + color2.getGreen() * inv);
        int b = (int) (color1.getBlue() * percent + color2.getBlue() * inv);
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    public static Color blend(Color c1, Color c2, double ratio) {
        float r = (float) ratio, ir = 1.0f - r;
        float[] rgb1 = new float[3], rgb2 = new float[3];
        c1.getColorComponents(rgb1);
        c2.getColorComponents(rgb2);
        return new Color(
                clampF(rgb1[0] * r + rgb2[0] * ir),
                clampF(rgb1[1] * r + rgb2[1] * ir),
                clampF(rgb1[2] * r + rgb2[2] * ir)
        );
    }

    public static Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions == null || colors == null || fractions.length != colors.length)
            throw new IllegalArgumentException("Fractions and colors must be non-null and equal length");
        int[] idx = getFraction(fractions, progress);
        float max = fractions[idx[1]] - fractions[idx[0]];
        float value = progress - fractions[idx[0]];
        float weight = value / max;
        return blend(colors[idx[0]], colors[idx[1]], 1.0f - weight);
    }

    // ─── String Utilities ─────────────────────────────────────

    public static String stripColor(String text) {
        return COLOR_PATTERN.matcher(text).replaceAll("");
    }

    // ─── ARGB Helpers ─────────────────────────────────────────

    public static int toARGB(Color c) {
        return (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    public static int toARGB(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static int interpolateColor(int c1, int c2, float factor) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        float inv = 1.0f - factor;
        return toARGB(
                (int) (r1 * inv + r2 * factor),
                (int) (g1 * inv + g2 * factor),
                (int) (b1 * inv + b2 * factor),
                (int) (a1 * inv + a2 * factor)
        );
    }

    // ─── Internal Helpers ─────────────────────────────────────

    private static int[] getFraction(float[] fractions, float progress) {
        int start = 0;
        for (; start < fractions.length && fractions[start] <= progress; start++) {}
        if (start >= fractions.length) start = fractions.length - 1;
        return new int[]{start - 1, start};
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static float clampF(float v) { return Math.max(0f, Math.min(1f, v)); }
}
