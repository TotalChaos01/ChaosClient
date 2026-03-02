package me.totalchaos01.chaosclient.ui.mainmenu;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.util.render.Animate;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.awt.*;

/**
 * Rise-style Main Menu for ChaosClient.
 * Features: dark background, centered client logo, rounded buttons (Singleplayer, Multiplayer, Settings),
 * theme selector at bottom, hover animations.
 *
 * Ported from Rise Client's MainMenu.java (470 lines).
 */
public class ChaosMainMenu extends Screen {

    // Button hover animation states
    private float singleHover = 0, multiHover = 0, settingsHover = 0, quitHover = 0;

    // Theme selector
    private final String[] themes = ThemeUtil.getThemeNames();
    private int selectedThemeIdx = 0;
    private float themeScrollX = 0;

    // Easter egg
    private final boolean isRice = Math.random() < 0.01; // 1% chance like Rise

    public ChaosMainMenu() {
        super(Text.literal("ChaosClient Main Menu"));
    }

    @Override
    protected void init() {
        super.init();
        // Find current theme index
        String currentTheme = ThemeUtil.getTheme();
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equals(currentTheme)) {
                selectedThemeIdx = i;
                break;
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // ─── Dark background with subtle gradient ─────────────
        ctx.fill(0, 0, width, height, 0xFF0D0D12);

        // Subtle radial gradient effect (Rise used panorama, we use gradient)
        Color themeColor = ThemeUtil.getThemeColor(ThemeType.LOGO);
        int gradientColor = ColorUtil.withAlpha(ColorUtil.toARGB(themeColor), 15);
        RenderUtil.gradientRect(ctx, 0, 0, width, height / 2.0, gradientColor, 0x00000000);
        RenderUtil.gradientRect(ctx, 0, height / 2.0, width, height / 2.0, 0x00000000, gradientColor);

        // ─── Client Logo (large, centered) ────────────────────
        String logoText = isRice ? "RiceClient" : ChaosClient.CLIENT_NAME;
        int logoColor = ColorUtil.toARGB(ThemeUtil.getThemeColor(0, ThemeType.LOGO, 1));

        ctx.getMatrices().pushMatrix();
        float scale = 5.0f;
        int logoW = client.textRenderer.getWidth(logoText);
        float logoX = (width / 2f - logoW * scale / 2f) / scale;
        float logoY = (height * 0.22f) / scale;
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(client.textRenderer, logoText, (int) logoX, (int) logoY, logoColor);
        ctx.getMatrices().popMatrix();

        // Version text below logo
        String version = "v" + ChaosClient.CLIENT_VERSION;
        int vw = client.textRenderer.getWidth(version);
        ctx.drawTextWithShadow(client.textRenderer, version, width / 2 - vw / 2, (int) (height * 0.22f + scale * 10 + 8), 0x80FFFFFF);

        // ─── Buttons ──────────────────────────────────────────
        float buttonWidth = 200;
        float buttonHeight = 28;
        float buttonX = width / 2f - buttonWidth / 2f;
        float buttonStartY = height * 0.45f;
        float buttonGap = 36;

        // Update hover animations
        singleHover = (float) Animate.lerp(singleHover, isInButton(mouseX, mouseY, buttonX, buttonStartY, buttonWidth, buttonHeight) ? 1 : 0, 0.2);
        multiHover = (float) Animate.lerp(multiHover, isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap, buttonWidth, buttonHeight) ? 1 : 0, 0.2);
        settingsHover = (float) Animate.lerp(settingsHover, isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 2, buttonWidth, buttonHeight) ? 1 : 0, 0.2);
        quitHover = (float) Animate.lerp(quitHover, isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 3, buttonWidth, buttonHeight) ? 1 : 0, 0.2);

        drawButton(ctx, "Singleplayer", buttonX, buttonStartY, buttonWidth, buttonHeight, singleHover, themeColor);
        drawButton(ctx, "Multiplayer", buttonX, buttonStartY + buttonGap, buttonWidth, buttonHeight, multiHover, themeColor);
        drawButton(ctx, "Settings", buttonX, buttonStartY + buttonGap * 2, buttonWidth, buttonHeight, settingsHover, themeColor);
        drawButton(ctx, "Quit", buttonX, buttonStartY + buttonGap * 3, buttonWidth, buttonHeight, quitHover, themeColor);

        // ─── Theme Selector (bottom bar) ──────────────────────
        renderThemeSelector(ctx, mouseX, mouseY);

        // ─── Bottom credits ───────────────────────────────────
        ctx.drawTextWithShadow(client.textRenderer, "ChaosClient " + ChaosClient.CLIENT_VERSION + " • Fabric 1.21.11", 4, height - 12, 0x60FFFFFF);
    }

    // ─── Button drawing ───────────────────────────────────────

    private void drawButton(DrawContext ctx, String text, float x, float y, float w, float h, float hover, Color themeColor) {
        // Background — gets brighter on hover
        int alpha = (int) (40 + hover * 50);
        int bgColor = ColorUtil.withAlpha(ColorUtil.toARGB(themeColor), alpha);

        // Border color interpolation
        int borderAlpha = (int) (60 + hover * 130);
        int borderColor = ColorUtil.withAlpha(ColorUtil.toARGB(themeColor), borderAlpha);

        // Draw rounded rect
        RenderUtil.roundedRectSimple(ctx, (int) x, (int) y, (int) w, (int) h, 8, bgColor);

        // Border outline
        RenderUtil.rectOutline(ctx, x, y, w, h, 1, borderColor);

        // Text centered
        int textColor = ColorUtil.interpolateColor(0xAAFFFFFF, 0xFFFFFFFF, hover);
        RenderUtil.drawCenteredText(ctx, text, (int) (x + w / 2), (int) (y + h / 2 - 4), textColor);
    }

    // ─── Theme Selector ───────────────────────────────────────

    private void renderThemeSelector(DrawContext ctx, int mouseX, int mouseY) {
        float selectorY = height - 35;
        float selectorH = 18;
        float totalW = 0;

        // Calculate total width
        for (String theme : themes) {
            totalW += client.textRenderer.getWidth(theme) + 16;
        }

        // Scroll target
        float targetScrollX = width / 2f;
        float consumed = 0;
        for (int i = 0; i < selectedThemeIdx; i++) {
            consumed += client.textRenderer.getWidth(themes[i]) + 16;
        }
        targetScrollX -= consumed + (client.textRenderer.getWidth(themes[selectedThemeIdx]) + 16) / 2f;
        themeScrollX = (float) Animate.lerp(themeScrollX, targetScrollX, 0.1);

        // Render themes
        float drawX = themeScrollX;
        for (int i = 0; i < themes.length; i++) {
            String theme = themes[i];
            int tw = client.textRenderer.getWidth(theme);
            float itemW = tw + 14;

            boolean selected = (i == selectedThemeIdx);
            boolean hovered = mouseX >= drawX && mouseX <= drawX + itemW &&
                    mouseY >= selectorY && mouseY <= selectorY + selectorH;

            // Background
            int bgAlpha = selected ? 100 : (hovered ? 50 : 20);
            Color themeColor = ThemeUtil.getThemeColor(ThemeType.GENERAL);
            int bg = ColorUtil.withAlpha(ColorUtil.toARGB(themeColor), bgAlpha);
            RenderUtil.roundedRectSimple(ctx, (int) drawX, (int) selectorY, (int) itemW, (int) selectorH, 4, bg);

            // Text
            int textColor = selected ? 0xFFFFFFFF : 0x90FFFFFF;
            ctx.drawTextWithShadow(client.textRenderer, theme, (int) (drawX + 7), (int) (selectorY + 5), textColor);

            drawX += itemW + 2;
        }
    }

    // ─── Input ────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x(), mouseY = click.y();
        float buttonWidth = 200;
        float buttonHeight = 28;
        float buttonX = width / 2f - buttonWidth / 2f;
        float buttonStartY = height * 0.45f;
        float buttonGap = 36;

        if (isInButton(mouseX, mouseY, buttonX, buttonStartY, buttonWidth, buttonHeight)) {
            client.setScreen(new SelectWorldScreen(this));
            return true;
        }
        if (isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap, buttonWidth, buttonHeight)) {
            client.setScreen(new MultiplayerScreen(this));
            return true;
        }
        if (isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 2, buttonWidth, buttonHeight)) {
            client.setScreen(new OptionsScreen(this, client.options));
            return true;
        }
        if (isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 3, buttonWidth, buttonHeight)) {
            client.scheduleStop();
            return true;
        }

        // Theme selector clicks
        float selectorY = height - 35;
        float selectorH = 18;
        float drawX = themeScrollX;
        for (int i = 0; i < themes.length; i++) {
            float itemW = client.textRenderer.getWidth(themes[i]) + 14;
            if (mouseX >= drawX && mouseX <= drawX + itemW &&
                    mouseY >= selectorY && mouseY <= selectorY + selectorH) {
                selectedThemeIdx = i;
                ThemeUtil.setTheme(themes[i]);
                return true;
            }
            drawX += itemW + 2;
        }

        return super.mouseClicked(click, bl);
    }

    private boolean isInButton(double mx, double my, double bx, double by, double bw, double bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
