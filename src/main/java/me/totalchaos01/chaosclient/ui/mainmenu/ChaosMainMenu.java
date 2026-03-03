package me.totalchaos01.chaosclient.ui.mainmenu;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.ui.clickgui.ClickGuiScreen;
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
 * ChaosClient Main Menu — Rise-style with settings panel,
 * theme selector, fade/transparency effects. Russian UI.
 */
public class ChaosMainMenu extends Screen {

    // Button hover animations
    private float singleHover = 0, multiHover = 0, settingsHover = 0, quitHover = 0;

    // Settings panel
    private boolean settingsPanelOpen = false;
    private final Animate settingsPanelAnim = new Animate(0, 0.10);
    private float themeScrollY = 0;

    // Fade-in animation
    private long openTime;
    private final Animate fadeAnim = new Animate(0, 0.06);

    // Ambient background animation
    private float bgPhase = 0;

    public ChaosMainMenu() {
        super(Text.literal("ChaosClient"));
    }

    @Override
    protected void init() {
        super.init();
        settingsPanelOpen = false;
        settingsPanelAnim.reset(0);
        themeScrollY = 0;
        openTime = System.currentTimeMillis();
        fadeAnim.setValue(0);
        fadeAnim.setTarget(1);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        fadeAnim.update();
        settingsPanelAnim.setTarget(settingsPanelOpen ? 1 : 0);
        settingsPanelAnim.update();
        float fade = (float) fadeAnim.getValue();
        bgPhase += delta * 0.3f;

        // Dark background
        ctx.fill(0, 0, width, height, 0xFF0D0D12);

        // Blur overlay
        RenderUtil.blurBackground(ctx, width, height, 6);

        // Animated ambient gradient (smooth — single pass, no stripe overlaps)
        Color themeColor = ThemeUtil.getThemeColor(bgPhase, ThemeType.LOGO, 0.5f);
        Color themeColor2 = ThemeUtil.getThemeColor(bgPhase + 3, ThemeType.LOGO, 0.5f);
        int g1 = ColorUtil.withAlpha(ColorUtil.toARGB(themeColor), (int) (14 * fade));
        int g2 = ColorUtil.withAlpha(ColorUtil.toARGB(themeColor2), (int) (10 * fade));

        // Single full-screen gradient (no stripes from overlapping rects)
        RenderUtil.gradientRect(ctx, 0, 0, width, height, g1, g2);

        // ─── Client name top-left ─────────────────────────────
        int logoAlpha = (int) (255 * fade);
        ctx.getMatrices().pushMatrix();
        float scale = 3.5f;
        ctx.getMatrices().scale(scale, scale);
        RenderUtil.drawGradientText(ctx, ChaosClient.CLIENT_NAME, (int)(14 / scale), (int)(12 / scale), 0, 1.5f);
        ctx.getMatrices().popMatrix();

        // Version under name
        String version = "v" + ChaosClient.CLIENT_VERSION;
        ctx.drawTextWithShadow(client.textRenderer, version, 16,
                (int)(12 + scale * 10 + 4), ColorUtil.withAlpha(0xFF999999, logoAlpha));

        // Player nick and MC version
        String playerNick = client.getSession() != null ? client.getSession().getUsername() : "Player";
        String mcVer = "Minecraft " + client.getGameVersion();
        ctx.drawTextWithShadow(client.textRenderer, "\u00A77\u0418\u0433\u0440\u043E\u043A: \u00A7f" + playerNick, 16,
                (int)(12 + scale * 10 + 16), ColorUtil.withAlpha(0xFFFFFFFF, (int)(200 * fade)));
        ctx.drawTextWithShadow(client.textRenderer, "\u00A78" + mcVer, 16,
                (int)(12 + scale * 10 + 28), ColorUtil.withAlpha(0xFF888888, (int)(180 * fade)));

        // Decorative theme line separating logo from buttons
        int lineY = (int)(height * 0.30f);
        int lineMargin = (int)(width * 0.25f);
        int lineAlpha = (int)(40 * fade);
        RenderUtil.gradientLine(ctx, lineMargin, lineY, width - lineMargin * 2, 1,
                ColorUtil.withAlpha(ColorUtil.toARGB(themeColor), lineAlpha),
                ColorUtil.withAlpha(ColorUtil.toARGB(themeColor2), lineAlpha));

        // Buttons (with fade-in stagger)
        float buttonWidth = 230;
        float buttonHeight = 36;
        float buttonX = width / 2f - buttonWidth / 2f;
        float buttonStartY = height * 0.35f;
        float buttonGap = 44;

        singleHover = (float) Animate.lerp(singleHover, isInButton(mouseX, mouseY, buttonX, buttonStartY, buttonWidth, buttonHeight) ? 1 : 0, 0.2);
        multiHover = (float) Animate.lerp(multiHover, isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap, buttonWidth, buttonHeight) ? 1 : 0, 0.2);
        settingsHover = (float) Animate.lerp(settingsHover, isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 2, buttonWidth, buttonHeight) ? 1 : 0, 0.2);
        quitHover = (float) Animate.lerp(quitHover, isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 3, buttonWidth, buttonHeight) ? 1 : 0, 0.2);

        // Staggered fade for each button
        float t1 = Math.min(1, (float) (System.currentTimeMillis() - openTime - 100) / 400);
        float t2 = Math.min(1, (float) (System.currentTimeMillis() - openTime - 200) / 400);
        float t3 = Math.min(1, (float) (System.currentTimeMillis() - openTime - 300) / 400);
        float t4 = Math.min(1, (float) (System.currentTimeMillis() - openTime - 400) / 400);

        if (t1 > 0) drawButton(ctx, "\u25B6  \u041E\u0434\u0438\u043D\u043E\u0447\u043D\u0430\u044F \u0438\u0433\u0440\u0430", buttonX, buttonStartY, buttonWidth, buttonHeight, singleHover, themeColor, t1);
        if (t2 > 0) drawButton(ctx, "\u2601  \u041C\u0443\u043B\u044C\u0442\u0438\u043F\u043B\u0435\u0435\u0440", buttonX, buttonStartY + buttonGap, buttonWidth, buttonHeight, multiHover, themeColor, t2);
        if (t3 > 0) drawButton(ctx, "\u2699  \u041D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438 \u043A\u043B\u0438\u0435\u043D\u0442\u0430", buttonX, buttonStartY + buttonGap * 2, buttonWidth, buttonHeight, settingsHover, themeColor, t3);
        if (t4 > 0) drawButton(ctx, "\u2716  \u0412\u044B\u0445\u043E\u0434", buttonX, buttonStartY + buttonGap * 3, buttonWidth, buttonHeight, quitHover, themeColor, t4);

        // Settings panel (slides from right)
        if (settingsPanelAnim.getValue() > 0.01) {
            renderSettingsPanel(ctx, mouseX, mouseY);
        }

        // Bottom credits
        ctx.drawTextWithShadow(client.textRenderer,
                "ChaosClient " + ChaosClient.CLIENT_VERSION + " \u2022 Fabric 1.21.11",
                4, height - 12, ColorUtil.withAlpha(0xFFFFFFFF, (int) (96 * fade)));

        // Hint text bottom-right
        ctx.drawTextWithShadow(client.textRenderer,
                "\u041F\u041A\u041C \u2014 \u043D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438 \u043A\u043B\u0438\u0435\u043D\u0442\u0430",
                width - client.textRenderer.getWidth("\u041F\u041A\u041C \u2014 \u043D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438 \u043A\u043B\u0438\u0435\u043D\u0442\u0430") - 4,
                height - 12, ColorUtil.withAlpha(0xFFFFFFFF, (int) (48 * fade)));
    }

    private void drawButton(DrawContext ctx, String text, float x, float y, float w, float h,
                            float hover, Color themeColor, float fadeIn) {
        // Slight Y offset for slide-in effect
        float offsetY = (1 - fadeIn) * 15;
        int iy = (int) (y + offsetY);
        int ix = (int) x;
        int iw = (int) w;
        int ih = (int) h;

        // Shadow behind button
        int shadowAlpha = (int) (25 * fadeIn);
        RenderUtil.shadow(ctx, ix, iy + 2, iw, ih, 10, ColorUtil.withAlpha(0xFF000000, shadowAlpha), 6, 1.5);

        // Background: dark base + subtle theme tint
        int baseBg = ColorUtil.withAlpha(0xFF0E0F18, (int) (220 * fadeIn));
        RenderUtil.roundedRectSimple(ctx, ix, iy, iw, ih, 10, baseBg);

        // Theme color overlay on hover
        int themeARGB = ColorUtil.toARGB(themeColor);
        int overlayAlpha = (int) ((8 + hover * 45) * fadeIn);
        RenderUtil.roundedRectSimple(ctx, ix, iy, iw, ih, 10, ColorUtil.withAlpha(themeARGB, overlayAlpha));

        // Top highlight strip for glass effect
        int highlightAlpha = (int) ((15 + hover * 25) * fadeIn);
        RenderUtil.roundedRectSimple(ctx, ix + 2, iy + 1, iw - 4, 1, 0,
                ColorUtil.withAlpha(0xFFFFFFFF, highlightAlpha));

        // Border — gradient outline from theme color
        int borderAlpha = (int) ((50 + hover * 140) * fadeIn);
        int borderColor = ColorUtil.withAlpha(themeARGB, borderAlpha);
        RenderUtil.roundedRectOutline(ctx, ix, iy, iw, ih, 10, 1, borderColor);

        // Inner glow on hover
        if (hover > 0.05f) {
            int glowAlpha = (int) (20 * hover * fadeIn);
            RenderUtil.roundedRectSimple(ctx, ix + 1, iy + 1, iw - 2, ih - 2, 9,
                    ColorUtil.withAlpha(themeARGB, glowAlpha));
        }

        // Text with hover brightness
        int textAlpha = (int) (255 * fadeIn);
        int textColor = ColorUtil.withAlpha(
                ColorUtil.interpolateColor(0xFFBBCCDD, 0xFFFFFFFF, hover), textAlpha);
        RenderUtil.drawCenteredText(ctx, text, (int) (x + w / 2), iy + ih / 2 - 4, textColor);
    }

    // --- Settings panel ---

    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
        float anim = (float) settingsPanelAnim.getValue();
        int panelW = 240;
        int panelH = 340;
        float panelX = width - panelW * anim - 10 + panelW * (1 - anim);
        float panelY = height / 2f - panelH / 2f;

        // Panel shadow + background
        RenderUtil.shadow(ctx, panelX, panelY, panelW, panelH, 14, 0xFF000000);
        RenderUtil.roundedRectSimple(ctx, (int) panelX, (int) panelY, panelW, panelH, 10, 0xEE101218);

        // Header
        ctx.drawTextWithShadow(client.textRenderer, "\u2699 \u041D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438",
                (int) panelX + 14, (int) panelY + 10, 0xFFEEEEFF);
        RenderUtil.gradientLine(ctx, (int) panelX + 12, (int) panelY + 22, panelW - 24, 1,
                ColorUtil.toARGB(ThemeUtil.getThemeColor(ThemeType.GENERAL)),
                ColorUtil.toARGB(ThemeUtil.getThemeColor(5, ThemeType.GENERAL)));

        int itemY = (int) panelY + 32;
        int itemH = 28;

        // Action buttons
        // 1. Open GUI
        boolean guiHover = mouseX >= panelX + 12 && mouseX <= panelX + panelW - 12 &&
                mouseY >= itemY && mouseY < itemY + itemH;
        RenderUtil.roundedRectSimple(ctx, (int) panelX + 12, itemY, panelW - 24, itemH - 2, 8,
                guiHover ? 0x30FFFFFF : 0x15FFFFFF);
        ctx.drawTextWithShadow(client.textRenderer, "\u25C9 \u041E\u0442\u043A\u0440\u044B\u0442\u044C ClickGUI",
                (int) panelX + 22, itemY + 9, guiHover ? 0xFFFFFFFF : 0xFFBBCCDD);

        itemY += itemH + 4;

        // 2. MC Settings
        boolean mcSettingsHover = mouseX >= panelX + 12 && mouseX <= panelX + panelW - 12 &&
                mouseY >= itemY && mouseY < itemY + itemH;
        RenderUtil.roundedRectSimple(ctx, (int) panelX + 12, itemY, panelW - 24, itemH - 2, 8,
                mcSettingsHover ? 0x30FFFFFF : 0x15FFFFFF);
        ctx.drawTextWithShadow(client.textRenderer, "\u2699 \u041D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438 MC",
                (int) panelX + 22, itemY + 9, mcSettingsHover ? 0xFFFFFFFF : 0xFFBBCCDD);

        itemY += itemH + 12;

        // Theme section header
        ctx.drawTextWithShadow(client.textRenderer, "\u25C9 \u0422\u0435\u043C\u044B",
                (int) panelX + 14, itemY, 0xFF99AABB);
        itemY += 14;

        // Theme list (scrollable)
        String[] themes = ThemeUtil.getThemeNames();
        String current = ThemeUtil.getTheme();
        int listH = panelH - (itemY - (int) panelY) - 10;

        ctx.enableScissor((int) panelX + 10, itemY, (int) panelX + panelW - 10, itemY + listH);

        float drawY = itemY + themeScrollY;
        int themeItemH = 22;
        for (int i = 0; i < themes.length; i++) {
            boolean selected = themes[i].equals(current);
            boolean hovered = mouseX >= panelX + 12 && mouseX <= panelX + panelW - 12 &&
                    mouseY >= drawY && mouseY < drawY + themeItemH;

            if (selected) {
                int accent = ColorUtil.toARGB(ThemeUtil.getThemeColor(ThemeType.GENERAL));
                RenderUtil.roundedRectSimple(ctx, (int) panelX + 12, (int) drawY,
                        panelW - 24, themeItemH - 2, 7, ColorUtil.withAlpha(accent, 80));
            } else if (hovered) {
                RenderUtil.roundedRectSimple(ctx, (int) panelX + 12, (int) drawY,
                        panelW - 24, themeItemH - 2, 7, 0x20FFFFFF);
            }

            int textColor = selected ? 0xFFFFFFFF : 0xFFBBCCDD;
            ctx.drawTextWithShadow(client.textRenderer, themes[i],
                    (int) panelX + 20, (int) drawY + 6, textColor);
            drawY += themeItemH;
        }

        ctx.disableScissor();
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x(), mouseY = click.y();
        int button = click.button();

        float buttonWidth = 230;
        float buttonHeight = 36;
        float buttonX = width / 2f - buttonWidth / 2f;
        float buttonStartY = height * 0.35f;
        float buttonGap = 44;

        if (isInButton(mouseX, mouseY, buttonX, buttonStartY, buttonWidth, buttonHeight)) {
            client.setScreen(new SelectWorldScreen(this));
            return true;
        }
        if (isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap, buttonWidth, buttonHeight)) {
            client.setScreen(new MultiplayerScreen(this));
            return true;
        }
        if (isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 2, buttonWidth, buttonHeight)) {
            settingsPanelOpen = !settingsPanelOpen;
            return true;
        }
        if (isInButton(mouseX, mouseY, buttonX, buttonStartY + buttonGap * 3, buttonWidth, buttonHeight)) {
            client.scheduleStop();
            return true;
        }

        // Settings panel clicks
        if (settingsPanelOpen && settingsPanelAnim.getValue() > 0.5) {
            float anim = (float) settingsPanelAnim.getValue();
            int panelW = 240;
            int panelH = 340;
            float panelXPos = width - panelW * anim - 10 + panelW * (1 - anim);
            float panelYPos = height / 2f - panelH / 2f;

            int itemYPos = (int) panelYPos + 32;
            int itemH = 28;

            // Open GUI button
            if (mouseX >= panelXPos + 12 && mouseX <= panelXPos + panelW - 12 &&
                    mouseY >= itemYPos && mouseY < itemYPos + itemH) {
                client.setScreen(new ClickGuiScreen());
                return true;
            }

            itemYPos += itemH + 4;

            // MC Settings button
            if (mouseX >= panelXPos + 12 && mouseX <= panelXPos + panelW - 12 &&
                    mouseY >= itemYPos && mouseY < itemYPos + itemH) {
                client.setScreen(new OptionsScreen(this, client.options));
                return true;
            }

            itemYPos += itemH + 12 + 14;

            // Theme list clicks
            String[] themes = ThemeUtil.getThemeNames();
            int themeItemH = 22;
            float drawY = itemYPos + themeScrollY;
            for (int i = 0; i < themes.length; i++) {
                if (mouseX >= panelXPos + 12 && mouseX <= panelXPos + panelW - 12 &&
                        mouseY >= drawY && mouseY < drawY + themeItemH) {
                    ThemeUtil.setTheme(themes[i]);
                    return true;
                }
                drawY += themeItemH;
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (settingsPanelOpen) {
            themeScrollY += (float) (vAmount * 20);
            if (themeScrollY > 0) themeScrollY = 0;
            float maxScroll = -(ThemeUtil.getThemeNames().length * 22 - 200);
            if (themeScrollY < maxScroll) themeScrollY = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    private boolean isInButton(double mx, double my, double bx, double by, double bw, double bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
