package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.module.impl.combat.KillAura;
import me.totalchaos01.chaosclient.util.render.Animate;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

/**
 * Rise-style TargetHUD — displays target name, health bar, distance,
 * and player face. Appears during combat (KillAura or manual hits),
 * fades out 5 seconds after last interaction.
 */
@ModuleInfo(name = "TargetHUD", description = "Shows target information during combat", category = Category.RENDER)
public class TargetHUD extends Module {

    private LivingEntity target;
    private long lastTargetTime;
    private float healthAnim;
    private float fadeAnim;
    private float absorptionAnim;

    private static final long FADE_OUT_DELAY = 5000; // 5s before fade starts
    private static final long FADE_DURATION = 500;   // 0.5s fade
    private static final int PANEL_W = 160;
    private static final int PANEL_H = 52;

    public TargetHUD() {}

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        DrawContext ctx = event.getDrawContext();

        // --- Find target ---
        LivingEntity newTarget = null;

        // 1. Check KillAura target
        KillAura killAura = ChaosClient.getInstance().getModuleManager().getModule(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getCurrentTarget() != null) {
            newTarget = killAura.getCurrentTarget();
        }

        // 2. Check crosshair target
        if (newTarget == null && mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
            newTarget = living;
        }

        // Update target tracking
        if (newTarget != null && newTarget.isAlive()) {
            target = newTarget;
            lastTargetTime = System.currentTimeMillis();
        }

        // --- Fade logic ---
        long elapsed = System.currentTimeMillis() - lastTargetTime;
        boolean shouldShow = target != null && target.isAlive() && elapsed < FADE_OUT_DELAY + FADE_DURATION;

        float targetFade = shouldShow ? 1.0f : 0.0f;
        if (shouldShow && elapsed > FADE_OUT_DELAY) {
            // Fading out
            targetFade = 1.0f - (float)(elapsed - FADE_OUT_DELAY) / FADE_DURATION;
        }
        fadeAnim = (float) Animate.lerp(fadeAnim, targetFade, 0.2);

        if (fadeAnim < 0.02f) {
            if (!shouldShow) target = null;
            return;
        }

        renderPanel(ctx);
    }

    private void renderPanel(DrawContext ctx) {
        if (target == null) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int x = screenW / 2 + 12;
        int y = screenH / 2 + 12;

        // Read position from HUD editor if available
        try {
            var mgr = ChaosClient.getInstance().getHudManager();
            if (mgr != null) {
                mgr.init();
                var el = mgr.get("targethud");
                if (el != null) {
                    x = (int) el.getX();
                    y = (int) el.getY();
                }
            }
        } catch (Exception ignored) {}

        int alpha = (int)(fadeAnim * 230);
        if (alpha < 5) return;

        Color theme = ThemeUtil.getThemeColor(0, ThemeType.GENERAL, 1);
        int themeARGB = ColorUtil.toARGB(theme);

        // Panel background with shadow
        RenderUtil.shadow(ctx, x, y, PANEL_W, PANEL_H, 8, ColorUtil.withAlpha(0xFF000000, alpha / 3), 5, 1.5);
        RenderUtil.roundedRectSimple(ctx, x, y, PANEL_W, PANEL_H, 8, ColorUtil.withAlpha(0xFF101018, alpha));

        // Theme accent line at top
        int accentAlpha = (int)(fadeAnim * 200);
        Color theme2 = ThemeUtil.getThemeColor(3, ThemeType.GENERAL, 1);
        RenderUtil.roundedRectGradientH(ctx, x, y, PANEL_W, 2, 1,
                ColorUtil.withAlpha(themeARGB, accentAlpha),
                ColorUtil.withAlpha(ColorUtil.toARGB(theme2), accentAlpha));

        // --- Player face (if player) or entity icon ---
        int faceSize = 24;
        int faceX = x + 5;
        int faceY = y + 6;

        if (target instanceof PlayerEntity player) {
            // Background behind face
            RenderUtil.roundedRectSimple(ctx, faceX - 1, faceY - 1, faceSize + 2, faceSize + 2, 4,
                    ColorUtil.withAlpha(0xFF1A1A2E, alpha));
            RenderUtil.drawPlayerFace(ctx, player, faceX, faceY, faceSize);
        } else {
            // Entity icon placeholder
            RenderUtil.roundedRectSimple(ctx, faceX - 1, faceY - 1, faceSize + 2, faceSize + 2, 4,
                    ColorUtil.withAlpha(0xFF1A1A2E, alpha));
            ctx.drawTextWithShadow(mc.textRenderer, "\u2666",
                    faceX + faceSize / 2 - 3, faceY + faceSize / 2 - 4,
                    ColorUtil.withAlpha(themeARGB, alpha));
        }

        // --- Target name ---
        String name = target.getName().getString();
        int textX = faceX + faceSize + 6;
        ctx.drawTextWithShadow(mc.textRenderer, name, textX, y + 6,
                ColorUtil.withAlpha(0xFFFFFFFF, alpha));

        // --- Health bar ---
        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float absorption = target.getAbsorptionAmount();
        float hpRatio = Math.max(0, Math.min(1, hp / Math.max(1, maxHp)));
        float absRatio = Math.max(0, Math.min(1, absorption / Math.max(1, maxHp)));

        // Smooth health animation
        healthAnim = (float) Animate.lerp(healthAnim, hpRatio, 0.12);
        absorptionAnim = (float) Animate.lerp(absorptionAnim, absRatio, 0.12);

        int barX = textX;
        int barY = y + 19;
        int barW = PANEL_W - (textX - x) - 8;
        int barH = 6;

        // Bar background
        RenderUtil.roundedRectSimple(ctx, barX, barY, barW, barH, 3,
                ColorUtil.withAlpha(0xFF1A1A2E, alpha));

        // Health fill (gradient from theme color to brighter)
        int healthColor = getHealthColor(healthAnim);
        int fillW = Math.max(0, (int)(barW * healthAnim));
        if (fillW > 0) {
            Color hc = new Color((healthColor >> 16) & 0xFF, (healthColor >> 8) & 0xFF, healthColor & 0xFF);
            Color hcBright = ColorUtil.brighter(hc, 0.3f);
            RenderUtil.roundedRectGradientH(ctx, barX, barY, fillW, barH, 3,
                    ColorUtil.withAlpha(ColorUtil.toARGB(hcBright), alpha),
                    ColorUtil.withAlpha(healthColor | 0xFF000000, alpha));
        }

        // Absorption overlay (golden)
        if (absorptionAnim > 0.01f) {
            int absW = Math.max(0, Math.min(barW - fillW, (int)(barW * absorptionAnim)));
            if (absW > 0) {
                RenderUtil.roundedRectSimple(ctx, barX + fillW, barY, absW, barH, 3,
                        ColorUtil.withAlpha(0xFFDDAA00, alpha));
            }
        }

        // --- HP text ---
        String hpText = String.format("%.1f", hp);
        if (absorption > 0) hpText += String.format(" +%.0f", absorption);
        hpText += String.format(" / %.0f", maxHp);
        ctx.drawTextWithShadow(mc.textRenderer, hpText, barX, barY + barH + 3,
                ColorUtil.withAlpha(0xFFAAAAAA, alpha));

        // --- Distance ---
        double dist = mc.player.distanceTo(target);
        String distText = String.format("%.1fm", dist);
        int distW = mc.textRenderer.getWidth(distText);
        ctx.drawTextWithShadow(mc.textRenderer, distText, x + PANEL_W - distW - 6, barY + barH + 3,
                ColorUtil.withAlpha(0xFF888888, alpha));
    }

    private int getHealthColor(float ratio) {
        // Green at full → Yellow at half → Red at low
        int r, g;
        if (ratio > 0.5f) {
            float t = (ratio - 0.5f) * 2f;
            r = (int)(255 * (1f - t));
            g = 255;
        } else {
            float t = ratio * 2f;
            r = 255;
            g = (int)(255 * t);
        }
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
