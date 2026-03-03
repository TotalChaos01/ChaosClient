package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

/**
 * Central Screen mixin — replaces the vanilla background and adds themed
 * decorations for MultiplayerScreen and SelectWorldScreen.
 * Uses instanceof checks so only the target screens are affected.
 */
@Mixin(Screen.class)
public abstract class MixinScreen {

    /**
     * Check if this screen should receive ChaosClient theming.
     */
    private boolean chaosclient$isThemedScreen() {
        Screen self = (Screen) (Object) this;
        return self instanceof MultiplayerScreen || self instanceof SelectWorldScreen;
    }

    /* ─── Replace vanilla background for themed screens ──────── */

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!chaosclient$isThemedScreen()) return;

        Screen self = (Screen) (Object) this;
        int w = self.width;
        int h = self.height;

        Color themeColor = ThemeUtil.getThemeColor(0, ThemeType.GENERAL, 1);
        Color themeColor2 = ThemeUtil.getThemeColor(5, ThemeType.GENERAL, 1);
        int accent = ColorUtil.toARGB(themeColor);
        int accent2 = ColorUtil.toARGB(themeColor2);

        // Dark blurred base
        RenderUtil.blurBackground(ctx, w, h, 6);
        ctx.fill(0, 0, w, h, 0xE00A0A12);

        // Top gradient glow
        for (int i = 0; i < 60; i++) {
            int alpha = (int) (30 * (1.0 - (double) i / 60));
            ctx.fill(0, i, w, i + 1, ColorUtil.withAlpha(accent, alpha));
        }

        // Bottom gradient glow
        for (int i = 0; i < 40; i++) {
            int alpha = (int) (20 * (1.0 - (double) i / 40));
            ctx.fill(0, h - i - 1, w, h - i, ColorUtil.withAlpha(accent2, alpha));
        }

        // Top accent line
        RenderUtil.gradientLine(ctx, 0, 0, w, 2, accent, accent2);

        // Bottom accent line
        RenderUtil.gradientLine(ctx, 0, h - 1, w, 1, accent2, accent);

        ci.cancel();
    }

    /* ─── Overlays on top of vanilla content ─────────────────── */

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!chaosclient$isThemedScreen()) return;

        Screen self = (Screen) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = self.width;
        int h = self.height;

        Color themeColor = ThemeUtil.getThemeColor(0, ThemeType.GENERAL, 1);
        Color themeColor2 = ThemeUtil.getThemeColor(5, ThemeType.GENERAL, 1);
        int accent = ColorUtil.toARGB(themeColor);
        int accent2 = ColorUtil.toARGB(themeColor2);

        // Client branding bottom-right
        String brand = ChaosClient.CLIENT_NAME + " v" + ChaosClient.CLIENT_VERSION;
        int bw = mc.textRenderer.getWidth(brand);
        ctx.drawTextWithShadow(mc.textRenderer, brand, w - bw - 6, h - 12,
                ColorUtil.withAlpha(accent, 120));

        // Corner decorations
        int cs = 12;
        ctx.fill(2, 4, 2 + cs, 5, ColorUtil.withAlpha(accent, 100));
        ctx.fill(2, 4, 3, 4 + cs, ColorUtil.withAlpha(accent, 100));
        ctx.fill(w - 2 - cs, 4, w - 2, 5, ColorUtil.withAlpha(accent2, 100));
        ctx.fill(w - 3, 4, w - 2, 4 + cs, ColorUtil.withAlpha(accent2, 100));
    }
}
