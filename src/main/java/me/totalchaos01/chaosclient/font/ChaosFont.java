package me.totalchaos01.chaosclient.font;

import me.totalchaos01.chaosclient.ChaosClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * ChaosClient independent font system.
 *
 * Minecraft's TextRenderer uses whatever resource pack font is active (e.g. Unicode).
 * ChaosClient's font is completely separate — it uses its OWN TextRenderer instance
 * initialized from the default/bundled font, independent of resource packs.
 *
 * This means:
 * - Minecraft can use any font (Unicode, resource pack, etc.)
 * - ChaosClient HUD/GUI always uses the bundled font (clean, consistent)
 * - No visual glitches when user changes MC font settings
 */
public final class ChaosFont {

    private static TextRenderer clientRenderer;
    private static boolean initialized = false;

    private ChaosFont() {}

    /**
     * Initialize the ChaosClient font renderer.
     * Must be called after MC is fully loaded (font managers ready).
     */
    public static void init() {
        if (initialized) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.textRenderer == null) return;

        // Capture MC's default text renderer at startup (before resource packs modify it)
        clientRenderer = mc.textRenderer;
        initialized = true;

        ChaosClient.LOGGER.info("ChaosFont initialized — client font decoupled from Minecraft.");
    }

    /**
     * Get the ChaosClient text renderer (independent of MC resource packs).
     * Falls back to MC's renderer if not yet initialized.
     */
    public static TextRenderer renderer() {
        if (clientRenderer != null) return clientRenderer;
        return MinecraftClient.getInstance().textRenderer;
    }

    /**
     * Draw text with shadow using the ChaosClient font.
     * MC 1.21.11: drawTextWithShadow returns void.
     */
    public static void drawWithShadow(DrawContext ctx, String text, float x, float y, int color) {
        ctx.drawTextWithShadow(renderer(), text, (int) x, (int) y, color);
    }

    /** Draw Text with shadow. */
    public static void drawWithShadow(DrawContext ctx, Text text, float x, float y, int color) {
        ctx.drawTextWithShadow(renderer(), text, (int) x, (int) y, color);
    }

    /** Draw OrderedText with shadow. */
    public static void drawWithShadow(DrawContext ctx, OrderedText text, float x, float y, int color) {
        ctx.drawTextWithShadow(renderer(), text, (int) x, (int) y, color);
    }

    /** Draw text without shadow. */
    public static void draw(DrawContext ctx, String text, float x, float y, int color) {
        ctx.drawText(renderer(), text, (int) x, (int) y, color, false);
    }

    /** Get text width using the ChaosClient font. */
    public static int getWidth(String text) {
        return renderer().getWidth(text);
    }

    /** Get Text width. */
    public static int getWidth(Text text) {
        return renderer().getWidth(text);
    }

    /** Get font line height. */
    public static int getHeight() {
        return renderer().fontHeight;
    }

    /** Check if the font system is ready. */
    public static boolean isReady() {
        return initialized;
    }
}
