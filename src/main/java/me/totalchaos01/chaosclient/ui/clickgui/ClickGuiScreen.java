package me.totalchaos01.chaosclient.ui.clickgui;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.setting.Setting;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.Animate;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rise-style ClickGUI — complete visual overhaul.
 * Features: animated category selector, gradient accents, glow effects,
 * iOS-style toggle switches, gradient sliders, theme customization panel,
 * smooth pop-in animation, dark/transparent mode, Rise-quality shadow.
 */
public class ClickGuiScreen extends Screen {

    // ─── Layout constants ─────────────────────────────────────
    private static final int CAT_WIDTH = 95;
    private static final int CAT_HEIGHT = 24;
    private static final int HEADER_H = 28;
    private static final int MODULE_H = 26;
    private static final int SETTING_H = 20;
    private static final int PAD = 6;

    // ─── Tabs ─────────────────────────────────────────────────
    private enum Tab { MODULES, THEMES }
    private Tab currentTab = Tab.MODULES;

    // ─── Window state (persists) ──────────────────────────────
    private float winX = -1, winY = -1;
    private float winW = 500, winH = 360;

    // ─── Animations ───────────────────────────────────────────
    private float popScale = 0.85f;
    private float renderSelectY = 0;
    private float scrollOffset = 0;
    private float themeScrollOffset = 0;
    private final Animate openAnim = new Animate(0.85, 0.08);
    private final Map<Module, Animate> moduleAnimX = new HashMap<>();
    private final Map<Module, Animate> moduleAnimAlpha = new HashMap<>();
    private final Map<String, Animate> toggleAnims = new HashMap<>();
    private final Map<String, Animate> sliderAnims = new HashMap<>();
    private float tabIndicatorX = 0;

    // ─── State ────────────────────────────────────────────────
    private Category selectedCat = Category.COMBAT;
    private final Map<Module, Boolean> expandedModules = new HashMap<>();
    private boolean dragging = false;
    private float dragOffX, dragOffY;
    private NumberSetting draggingSlider = null;
    private float sliderLeft, sliderWidth;
    private long openTime;

    // ─── Colors (computed from theme) ─────────────────────────
    private int colorBg, colorSidebar, colorHeader, colorAccent, colorAccentDark;
    private int colorAccent2; // secondary gradient color

    // ─── Category icons ───────────────────────────────────────
    private static final Map<Category, String> ICONS = Map.of(
            Category.COMBAT, "\u2694",
            Category.MOVEMENT, "\u27A4",
            Category.PLAYER, "\u263A",
            Category.RENDER, "\u25C9",
            Category.GHOST, "\uD83D\uDC7B",
            Category.OTHER, "\u2699"
    );

    // ─── Theme customization settings (integrated here) ───────
    // These are persisted as settings on the ClickGui module but managed here
    private static float gradientSpeed = 1.0f;
    private static float pulseFrequency = 1.0f;
    private static float glowIntensity = 1.0f;
    private static boolean darkMode = true;
    private static boolean shadowEnabled = true;
    private static boolean glowEnabled = true;
    private static int selectedThemeIndex = 0;

    public ClickGuiScreen() {
        super(Text.literal("ChaosClient"));
    }

    // ═══════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════

    @Override
    protected void init() {
        super.init();
        openAnim.setValue(0.85);
        openAnim.setTarget(1.0);
        dragging = false;
        draggingSlider = null;
        openTime = System.currentTimeMillis();

        if (winX < 0 || winY < 0) {
            winX = this.width / 2f - winW / 2f;
            winY = this.height / 2f - winH / 2f;
        }

        updateColors();
    }

    private void updateColors() {
        Color theme = ThemeUtil.getThemeColor(0, ThemeType.GENERAL, gradientSpeed);
        Color theme2 = ThemeUtil.getThemeColor(5, ThemeType.GENERAL, gradientSpeed);

        colorAccent = ColorUtil.toARGB(theme);
        colorAccent2 = ColorUtil.toARGB(theme2);
        colorAccentDark = ColorUtil.toARGB(ColorUtil.darker(theme, 0.6));

        if (darkMode) {
            colorBg = 0xFF171A21;
            colorSidebar = 0xFF121419;
            colorHeader = 0xFF171A21;
        } else {
            colorBg = 0xFF272A30;
            colorSidebar = 0xFF26272C;
            colorHeader = 0xFF272A30;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        updateColors();
        openAnim.update();
        float scale = (float) openAnim.getValue();

        // ── Dark overlay ──────────────────────────────────────
        int overlayAlpha = (int) (0x70 * Math.min(1.0, (System.currentTimeMillis() - openTime) / 300.0));
        ctx.fill(0, 0, this.width, this.height, (overlayAlpha << 24));

        ctx.getMatrices().pushMatrix();

        // Scale from center
        float cx = winX + winW / 2f, cy = winY + winH / 2f;
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(-cx, -cy);

        // ── Shadow ────────────────────────────────────────────
        if (shadowEnabled) {
            RenderUtil.shadow(ctx, winX, winY, winW, winH, 12, 0xFF000000, 14, 2.5);
        }

        // ── Accent glow behind window ─────────────────────────
        if (glowEnabled) {
            Color glowCol = new Color((colorAccent >> 16) & 0xFF, (colorAccent >> 8) & 0xFF,
                    colorAccent & 0xFF, 40);
            RenderUtil.glow(ctx, winX, winY, winW, winH, 12, glowCol, (int)(6 * glowIntensity));
        }

        // ── Sidebar background ────────────────────────────────
        RenderUtil.roundedRectSimple(ctx, (int) winX, (int) winY, CAT_WIDTH, (int) winH, 12, colorSidebar);
        ctx.fill((int) (winX + CAT_WIDTH - 12), (int) winY, (int) (winX + CAT_WIDTH), (int) (winY + winH), colorSidebar);

        // ── Module area background ────────────────────────────
        RenderUtil.roundedRectSimple(ctx, (int) (winX + CAT_WIDTH), (int) winY,
                (int) (winW - CAT_WIDTH), (int) winH, 12, colorBg);
        ctx.fill((int) (winX + CAT_WIDTH), (int) winY,
                (int) (winX + CAT_WIDTH + 12), (int) (winY + winH), colorBg);

        // ── Header ────────────────────────────────────────────
        RenderUtil.roundedRectSimple(ctx, (int) (winX + CAT_WIDTH), (int) winY,
                (int) (winW - CAT_WIDTH), HEADER_H, 12, colorHeader);
        ctx.fill((int) (winX + CAT_WIDTH), (int) (winY + HEADER_H - 6),
                (int) (winX + winW), (int) (winY + HEADER_H), colorHeader);

        // Header gradient accent line
        RenderUtil.gradientLine(ctx, (int) (winX + CAT_WIDTH), (int) (winY + HEADER_H - 1),
                (int) (winW - CAT_WIDTH), 1, colorAccent, colorAccent2);

        // Logo
        RenderUtil.drawGradientText(ctx, ChaosClient.CLIENT_NAME,
                (int) (winX + CAT_WIDTH + 12), (int) (winY + 9), 0, 1.5f);
        int nameW = client.textRenderer.getWidth(ChaosClient.CLIENT_NAME);
        ctx.drawTextWithShadow(client.textRenderer, " v" + ChaosClient.CLIENT_VERSION,
                (int) (winX + CAT_WIDTH + 14 + nameW), (int) (winY + 9), 0xFF555566);

        // Tab buttons (Modules / Themes)
        renderTabs(ctx, mouseX, mouseY);

        // ── Category sidebar ──────────────────────────────────
        renderCategories(ctx, mouseX, mouseY);

        // ── Content area ──────────────────────────────────────
        int contentX = (int) (winX + CAT_WIDTH + 1);
        int contentY = (int) (winY + HEADER_H);
        int contentW = (int) (winW - CAT_WIDTH - 1);
        int contentH = (int) (winH - HEADER_H);

        ctx.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        if (currentTab == Tab.MODULES) {
            renderModules(ctx, mouseX, mouseY, contentX, contentY, contentW);
        } else {
            renderThemePanel(ctx, mouseX, mouseY, contentX, contentY, contentW, contentH);
        }

        ctx.disableScissor();

        ctx.getMatrices().popMatrix();
    }

    // ─── Tab buttons ──────────────────────────────────────────

    private void renderTabs(DrawContext ctx, int mouseX, int mouseY) {
        int tabY = (int) (winY + 7);
        int tabBaseX = (int) (winX + winW - 150);

        String[] tabNames = {"Modules", "Themes"};
        Tab[] tabs = {Tab.MODULES, Tab.THEMES};

        float targetIndicatorX = currentTab == Tab.MODULES ? tabBaseX : tabBaseX + 70;
        tabIndicatorX = (float) Animate.lerp(tabIndicatorX, targetIndicatorX, 0.15);

        // Animated underline
        RenderUtil.roundedRectGradientH(ctx, (int) tabIndicatorX, tabY + 12, 60, 2, 1, colorAccent, colorAccent2);

        for (int i = 0; i < tabNames.length; i++) {
            int tx = tabBaseX + i * 70;
            boolean active = tabs[i] == currentTab;
            boolean hovered = mouseX >= tx && mouseX <= tx + 60 && mouseY >= tabY && mouseY <= tabY + 14;

            int textColor = active ? 0xFFFFFFFF : (hovered ? 0xFFBBBBCC : 0xFF666677);
            ctx.drawTextWithShadow(client.textRenderer, tabNames[i], tx + 10, tabY, textColor);
        }
    }

    // ─── Category sidebar ─────────────────────────────────────

    private void renderCategories(DrawContext ctx, int mouseX, int mouseY) {
        Category[] cats = Category.values();

        int targetIdx = 0;
        for (int i = 0; i < cats.length; i++) {
            if (cats[i] == selectedCat) { targetIdx = i; break; }
        }
        float targetY = HEADER_H + CAT_HEIGHT * targetIdx + 4;
        renderSelectY = (float) Animate.lerp(renderSelectY, targetY, 0.15);

        // Animated selection bar with gradient
        RenderUtil.roundedRectGradientH(ctx,
                (int) winX + 3, (int) (winY + renderSelectY),
                CAT_WIDTH - 6, CAT_HEIGHT, 6, colorAccent, colorAccent2);

        // Glow behind selected category
        if (glowEnabled) {
            Color gl = new Color((colorAccent >> 16) & 0xFF, (colorAccent >> 8) & 0xFF,
                    colorAccent & 0xFF, 30);
            RenderUtil.glow(ctx, winX + 3, winY + renderSelectY, CAT_WIDTH - 6, CAT_HEIGHT, 6, gl, 4);
        }

        for (int i = 0; i < cats.length; i++) {
            float catY = winY + HEADER_H + CAT_HEIGHT * i + 4;
            String icon = ICONS.getOrDefault(cats[i], "\u2022");
            String name = cats[i].getDisplayName();

            boolean hovered = mouseX >= winX && mouseX <= winX + CAT_WIDTH &&
                    mouseY >= catY && mouseY < catY + CAT_HEIGHT;
            boolean selected = cats[i] == selectedCat;

            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFCCCCDD : 0xFFAABBCC);

            ctx.drawTextWithShadow(client.textRenderer, icon, (int) (winX + 8), (int) (catY + 7), textColor);
            ctx.drawTextWithShadow(client.textRenderer, name, (int) (winX + 22), (int) (catY + 7), textColor);
        }
    }

    // ─── Module list ──────────────────────────────────────────

    private void renderModules(DrawContext ctx, int mouseX, int mouseY, int areaX, int areaY, int areaW) {
        List<Module> modules = ChaosClient.getInstance().getModuleManager().getModulesByCategory(selectedCat);
        float my = areaY + PAD + scrollOffset;

        for (int idx = 0; idx < modules.size(); idx++) {
            Module module = modules.get(idx);
            float moduleY = my;

            // Calculate content height
            float contentH = MODULE_H;
            if (expandedModules.getOrDefault(module, false)) {
                contentH = MODULE_H + 4;
                for (Setting s : module.getSettings()) contentH += SETTING_H;
            }

            // Skip if off-screen
            if (moduleY + contentH < areaY - 10 || moduleY > areaY + winH) {
                my += contentH + 4;
                continue;
            }

            boolean hovered = mouseX >= areaX + 4 && mouseX <= areaX + areaW - 4 &&
                    mouseY >= moduleY && mouseY < moduleY + contentH;
            boolean expanded = expandedModules.getOrDefault(module, false);

            // Module background
            int bgColor;
            if (module.isEnabled()) {
                // Gradient accent bg for enabled modules
                int accentAlpha = ColorUtil.withAlpha(colorAccent, 35);
                int accentAlpha2 = ColorUtil.withAlpha(colorAccent2, 20);
                RenderUtil.roundedRectGradientH(ctx, areaX + 4, (int) moduleY,
                        areaW - 8, (int) contentH, 8, accentAlpha, accentAlpha2);
            } else if (hovered) {
                RenderUtil.roundedRectSimple(ctx, areaX + 4, (int) moduleY,
                        areaW - 8, (int) contentH, 8, 0x15FFFFFF);
            }

            // Left accent bar for enabled
            if (module.isEnabled()) {
                RenderUtil.roundedRectGradientV(ctx, areaX + 4, (int) moduleY + 3,
                        3, MODULE_H - 6, 1, colorAccent, colorAccent2);
            }

            // Module name
            int textColor = module.isEnabled() ? 0xFFFFFFFF : 0xFFBBBBCC;
            ctx.drawTextWithShadow(client.textRenderer, module.getName(),
                    areaX + 14, (int) (moduleY + 8), textColor);

            // Description on hover
            if (hovered && !expanded && module.getDescription() != null) {
                ctx.drawTextWithShadow(client.textRenderer,
                        "\u00A77" + module.getDescription(),
                        areaX + 14 + client.textRenderer.getWidth(module.getName()) + 6,
                        (int) (moduleY + 8), 0xFF555566);
            }

            // Toggle switch (right side)
            String toggleKey = module.getName() + "_toggle";
            Animate toggleAnim = toggleAnims.computeIfAbsent(toggleKey, k -> new Animate(module.isEnabled() ? 1 : 0, 0.12));
            toggleAnim.setTarget(module.isEnabled() ? 1 : 0);
            toggleAnim.update();
            int switchX = areaX + areaW - 42;
            int switchY = (int) (moduleY + 6);
            RenderUtil.toggleSwitch(ctx, switchX, switchY, 28, 14,
                    (float) toggleAnim.getValue(), 0xFF3A3D47, colorAccent);

            // Settings expand arrow
            if (!module.getSettings().isEmpty()) {
                String arrow = expanded ? "\u25BE" : "\u25B8";
                ctx.drawTextWithShadow(client.textRenderer, arrow,
                        areaX + areaW - 54, (int) (moduleY + 8), 0xFF666677);
            }

            // Keybind
            if (module.getKeyBind() != 0) {
                String key = org.lwjgl.glfw.GLFW.glfwGetKeyName(module.getKeyBind(), 0);
                if (key != null) {
                    String keyText = "[" + key.toUpperCase() + "]";
                    int kw = client.textRenderer.getWidth(keyText);
                    ctx.drawTextWithShadow(client.textRenderer, "\u00A78" + keyText,
                            areaX + areaW - 56 - kw, (int) (moduleY + 8), 0xFF444455);
                }
            }

            // ─── Settings (expanded) ──────────────────────────
            if (expanded) {
                float settingY = moduleY + MODULE_H + 2;

                for (Setting setting : module.getSettings()) {
                    renderSetting(ctx, setting, module, areaX + 18, settingY, areaW - 36, mouseX, mouseY);
                    settingY += SETTING_H;
                }
            }

            my += contentH + 4;
        }
    }

    // ─── Setting rendering ────────────────────────────────────

    private void renderSetting(DrawContext ctx, Setting setting, Module module,
                               int x, float y, int width, int mouseX, int mouseY) {
        // Subtle setting background
        RenderUtil.roundedRectSimple(ctx, x, (int) y, width, SETTING_H - 2, 4, 0x0CFFFFFF);

        if (setting instanceof BooleanSetting bs) {
            ctx.drawTextWithShadow(client.textRenderer, setting.getName(),
                    x + 6, (int) (y + 5), 0xFFDDDDEE);

            // Animated toggle switch
            String key = module.getName() + "_" + setting.getName();
            Animate anim = toggleAnims.computeIfAbsent(key, k -> new Animate(bs.isEnabled() ? 1 : 0, 0.12));
            anim.setTarget(bs.isEnabled() ? 1 : 0);
            anim.update();

            int switchX = x + width - 30;
            int switchY = (int) (y + 3);
            RenderUtil.toggleSwitch(ctx, switchX, switchY, 24, 12,
                    (float) anim.getValue(), 0xFF3A3D47, colorAccent);

        } else if (setting instanceof NumberSetting ns) {
            String val = ns.getIncrement() >= 1
                    ? String.valueOf((int) ns.getValue())
                    : String.format("%.2f", ns.getValue());

            ctx.drawTextWithShadow(client.textRenderer, setting.getName(),
                    x + 6, (int) (y + 2), 0xFFDDDDEE);
            int vw = client.textRenderer.getWidth(val);
            ctx.drawTextWithShadow(client.textRenderer, val,
                    x + width - vw - 6, (int) (y + 2), colorAccent);

            // Animated slider with gradient fill
            float sliderY = y + 13;
            float sliderW = width - 12;
            float percent = (float) ((ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin()));

            // Animated slider position
            String sliderKey = module.getName() + "_" + setting.getName() + "_s";
            Animate sliderAnim = sliderAnims.computeIfAbsent(sliderKey, k -> new Animate(percent, 0.15));
            sliderAnim.setTarget(percent);
            sliderAnim.update();
            float animPercent = (float) sliderAnim.getValue();

            // Track
            RenderUtil.roundedRectSimple(ctx, x + 6, (int) sliderY, (int) sliderW, 4, 2, 0xFF2A2D37);

            // Fill with gradient
            int fillW = (int) (sliderW * animPercent);
            if (fillW > 0) {
                RenderUtil.roundedRectGradientH(ctx, x + 6, (int) sliderY, fillW, 4, 2, colorAccent, colorAccent2);
            }

            // Knob
            int knobX = x + 6 + (int) (sliderW * animPercent) - 3;
            RenderUtil.circle(ctx, knobX + 3, sliderY + 2, 4, 0xFFFFFFFF);

            // Slider drag handling
            if (draggingSlider == ns) {
                float startX = x + 6;
                float mxClamped = Math.max(startX, Math.min(mouseX, startX + sliderW));
                double pct = (mxClamped - startX) / sliderW;
                double newVal = ns.getMin() + pct * (ns.getMax() - ns.getMin());
                ns.setValue(newVal);
            }

        } else if (setting instanceof ModeSetting ms) {
            ctx.drawTextWithShadow(client.textRenderer, setting.getName(),
                    x + 6, (int) (y + 5), 0xFFDDDDEE);

            // Mode pill with accent background
            String mode = ms.getMode();
            int mw = client.textRenderer.getWidth(mode);
            int pillX = x + width - mw - 14;
            int pillY = (int) (y + 2);
            RenderUtil.roundedRectSimple(ctx, pillX, pillY, mw + 10, 14, 7,
                    ColorUtil.withAlpha(colorAccent, 50));
            ctx.drawTextWithShadow(client.textRenderer, mode, pillX + 5, pillY + 3, colorAccent);
        }
    }

    // ─── Theme customization panel ────────────────────────────

    private void renderThemePanel(DrawContext ctx, int mouseX, int mouseY,
                                  int areaX, int areaY, int areaW, int areaH) {
        float py = areaY + PAD + themeScrollOffset;
        int pad = 12;
        int itemH = 30;

        // Title
        ctx.drawTextWithShadow(client.textRenderer, "\u00A7lTheme Customization",
                areaX + pad, (int) py + 4, 0xFFEEEEFF);
        py += 24;

        // Gradient accent line
        RenderUtil.gradientLine(ctx, areaX + pad, (int) py, areaW - pad * 2, 1, colorAccent, colorAccent2);
        py += 8;

        // ── Theme selector grid ───────────────────────────────
        ctx.drawTextWithShadow(client.textRenderer, "Color Theme",
                areaX + pad, (int) py, 0xFFBBBBCC);
        py += 14;

        String[] themes = ThemeUtil.getThemeNames();
        int cols = 3;
        int cellW = (areaW - pad * 2 - (cols - 1) * 4) / cols;
        int cellH = 22;

        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int tx = areaX + pad + col * (cellW + 4);
            int ty = (int) py + row * (cellH + 4);

            boolean selected = themes[i].equals(ThemeUtil.getTheme());
            boolean hovered = mouseX >= tx && mouseX <= tx + cellW && mouseY >= ty && mouseY <= ty + cellH;

            // Theme preview — use the theme's own colors
            String savedTheme = ThemeUtil.getTheme();
            ThemeUtil.setTheme(themes[i]);
            Color c1 = ThemeUtil.getThemeColor(0, ThemeType.GENERAL, 1);
            Color c2 = ThemeUtil.getThemeColor(5, ThemeType.GENERAL, 1);
            ThemeUtil.setTheme(savedTheme);

            int bg = selected ? ColorUtil.withAlpha(ColorUtil.toARGB(c1), 80)
                    : (hovered ? 0x25FFFFFF : 0x12FFFFFF);
            RenderUtil.roundedRectSimple(ctx, tx, ty, cellW, cellH, 6, bg);

            if (selected) {
                RenderUtil.roundedRectOutline(ctx, tx, ty, cellW, cellH, 6, 1, ColorUtil.toARGB(c1));
                // Mini gradient preview bar
                RenderUtil.roundedRectGradientH(ctx, tx + 3, ty + cellH - 5, cellW - 6, 3, 1,
                        ColorUtil.toARGB(c1), ColorUtil.toARGB(c2));
            }

            // Theme name (truncated if too wide)
            String displayName = themes[i];
            if (client.textRenderer.getWidth(displayName) > cellW - 8) {
                while (client.textRenderer.getWidth(displayName + "..") > cellW - 8 && displayName.length() > 1) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName += "..";
            }
            ctx.drawTextWithShadow(client.textRenderer, displayName, tx + 4, ty + 7,
                    selected ? 0xFFFFFFFF : 0xFFBBBBCC);
        }

        py += ((themes.length + cols - 1) / cols) * (cellH + 4) + 12;

        // ── Sliders ───────────────────────────────────────────
        py = renderThemeSlider(ctx, "Gradient Speed", gradientSpeed, 0.1f, 3.0f, 0.1f,
                areaX + pad, (int) py, areaW - pad * 2, mouseX, mouseY, "gradientSpeed");
        py = renderThemeSlider(ctx, "Pulse Frequency", pulseFrequency, 0.1f, 3.0f, 0.1f,
                areaX + pad, (int) py, areaW - pad * 2, mouseX, mouseY, "pulseFrequency");
        py = renderThemeSlider(ctx, "Glow Intensity", glowIntensity, 0.0f, 2.0f, 0.1f,
                areaX + pad, (int) py, areaW - pad * 2, mouseX, mouseY, "glowIntensity");

        py += 8;

        // ── Toggle options ────────────────────────────────────
        py = renderThemeToggle(ctx, "Dark Mode", darkMode, areaX + pad, (int) py, areaW - pad * 2, mouseX, mouseY, "darkMode");
        py = renderThemeToggle(ctx, "Shadow", shadowEnabled, areaX + pad, (int) py, areaW - pad * 2, mouseX, mouseY, "shadow");
        py = renderThemeToggle(ctx, "Glow Effects", glowEnabled, areaX + pad, (int) py, areaW - pad * 2, mouseX, mouseY, "glow");
    }

    private float renderThemeSlider(DrawContext ctx, String name, float value, float min, float max,
                                    float inc, int x, int y, int width, int mx, int my, String id) {
        ctx.drawTextWithShadow(client.textRenderer, name, x, y + 2, 0xFFDDDDEE);
        String valStr = String.format("%.1f", value);
        int vw = client.textRenderer.getWidth(valStr);
        ctx.drawTextWithShadow(client.textRenderer, valStr, x + width - vw, y + 2, colorAccent);

        int sliderY = y + 14;
        float percent = (value - min) / (max - min);

        // Track
        RenderUtil.roundedRectSimple(ctx, x, sliderY, width, 4, 2, 0xFF2A2D37);

        // Fill
        int fillW = (int) (width * percent);
        if (fillW > 0) {
            RenderUtil.roundedRectGradientH(ctx, x, sliderY, fillW, 4, 2, colorAccent, colorAccent2);
        }

        // Knob
        RenderUtil.circle(ctx, x + width * percent, sliderY + 2, 4, 0xFFFFFFFF);

        return y + 28;
    }

    private float renderThemeToggle(DrawContext ctx, String name, boolean value,
                                    int x, int y, int width, int mx, int my, String id) {
        ctx.drawTextWithShadow(client.textRenderer, name, x, y + 3, 0xFFDDDDEE);

        Animate anim = toggleAnims.computeIfAbsent("theme_" + id, k -> new Animate(value ? 1 : 0, 0.12));
        anim.setTarget(value ? 1 : 0);
        anim.update();

        RenderUtil.toggleSwitch(ctx, x + width - 28, y, 28, 14,
                (float) anim.getValue(), 0xFF3A3D47, colorAccent);

        return y + 22;
    }

    // ═══════════════════════════════════════════════════════════
    //  INPUT
    // ═══════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mx = (int) click.x(), my = (int) click.y();
        int button = click.button();

        // ─── Tab buttons ──────────────────────────────────────
        int tabY = (int) (winY + 7);
        int tabBaseX = (int) (winX + winW - 150);
        if (my >= tabY && my <= tabY + 14) {
            if (mx >= tabBaseX && mx <= tabBaseX + 60) {
                currentTab = Tab.MODULES;
                return true;
            }
            if (mx >= tabBaseX + 70 && mx <= tabBaseX + 130) {
                currentTab = Tab.THEMES;
                return true;
            }
        }

        // ─── Header drag ──────────────────────────────────────
        if (mx >= winX + CAT_WIDTH && mx <= winX + winW &&
                my >= winY && my <= winY + HEADER_H) {
            dragging = true;
            dragOffX = mx - winX;
            dragOffY = my - winY;
            return true;
        }

        // ─── Category clicks ──────────────────────────────────
        if (mx >= winX && mx <= winX + CAT_WIDTH && my >= winY + HEADER_H) {
            Category[] cats = Category.values();
            for (int i = 0; i < cats.length; i++) {
                float catY = winY + HEADER_H + CAT_HEIGHT * i + 4;
                if (my >= catY && my < catY + CAT_HEIGHT) {
                    selectedCat = cats[i];
                    scrollOffset = 0;
                    currentTab = Tab.MODULES;
                    return true;
                }
            }
        }

        // ─── Theme panel clicks ───────────────────────────────
        if (currentTab == Tab.THEMES) {
            return handleThemeClicks(mx, my, button);
        }

        // ─── Module clicks ────────────────────────────────────
        return handleModuleClicks(mx, my, button, click, bl);
    }

    private boolean handleThemeClicks(int mx, int my, int button) {
        int areaX = (int) (winX + CAT_WIDTH + 1);
        int areaY = (int) (winY + HEADER_H);
        int areaW = (int) (winW - CAT_WIDTH - 1);
        int pad = 12;
        float py = areaY + PAD + themeScrollOffset + 24 + 8 + 14;

        // Theme grid clicks
        String[] themes = ThemeUtil.getThemeNames();
        int cols = 3;
        int cellW = (areaW - pad * 2 - (cols - 1) * 4) / cols;
        int cellH = 22;

        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int tx = areaX + pad + col * (cellW + 4);
            int ty = (int) py + row * (cellH + 4);

            if (mx >= tx && mx <= tx + cellW && my >= ty && my <= ty + cellH) {
                ThemeUtil.setTheme(themes[i]);
                // Also update the HUD theme setting
                Setting s = ChaosClient.getInstance().getModuleManager().getSetting("HUD", "Theme");
                if (s instanceof ModeSetting ms) ms.setMode(themes[i]);
                return true;
            }
        }

        py += ((themes.length + cols - 1) / cols) * (cellH + 4) + 12;

        // Slider clicks for theme settings
        int sliderHeight = 28;
        // Gradient Speed
        if (my >= py && my < py + sliderHeight && mx >= areaX + pad && mx <= areaX + areaW - pad) {
            float pct = (float) (mx - areaX - pad) / (areaW - pad * 2);
            gradientSpeed = Math.max(0.1f, Math.min(3.0f, 0.1f + pct * 2.9f));
            gradientSpeed = Math.round(gradientSpeed * 10) / 10f;
            return true;
        }
        py += sliderHeight;
        // Pulse Frequency
        if (my >= py && my < py + sliderHeight && mx >= areaX + pad && mx <= areaX + areaW - pad) {
            float pct = (float) (mx - areaX - pad) / (areaW - pad * 2);
            pulseFrequency = Math.max(0.1f, Math.min(3.0f, 0.1f + pct * 2.9f));
            pulseFrequency = Math.round(pulseFrequency * 10) / 10f;
            return true;
        }
        py += sliderHeight;
        // Glow Intensity
        if (my >= py && my < py + sliderHeight && mx >= areaX + pad && mx <= areaX + areaW - pad) {
            float pct = (float) (mx - areaX - pad) / (areaW - pad * 2);
            glowIntensity = Math.max(0.0f, Math.min(2.0f, pct * 2.0f));
            glowIntensity = Math.round(glowIntensity * 10) / 10f;
            return true;
        }
        py += sliderHeight + 8;

        // Toggle clicks
        int toggleW = areaW - pad * 2;
        int toggleH = 22;
        if (my >= py && my < py + toggleH) { darkMode = !darkMode; return true; }
        py += toggleH;
        if (my >= py && my < py + toggleH) { shadowEnabled = !shadowEnabled; return true; }
        py += toggleH;
        if (my >= py && my < py + toggleH) { glowEnabled = !glowEnabled; return true; }

        return false;
    }

    private boolean handleModuleClicks(int mx, int my, int button, Click click, boolean bl) {
        List<Module> modules = ChaosClient.getInstance().getModuleManager().getModulesByCategory(selectedCat);
        float modY = winY + HEADER_H + PAD + scrollOffset;
        int areaX = (int) (winX + CAT_WIDTH + 1);
        int areaW = (int) (winW - CAT_WIDTH - 1);

        for (Module module : modules) {
            float contentH = MODULE_H;
            if (expandedModules.getOrDefault(module, false)) {
                contentH = MODULE_H + 4;
                for (Setting s : module.getSettings()) contentH += SETTING_H;
            }

            // Module header click — toggle switch area
            if (mx >= areaX + areaW - 46 && mx <= areaX + areaW - 14 &&
                    my >= modY && my < modY + MODULE_H) {
                module.toggle();
                return true;
            }

            // Module header click — left side toggles settings
            if (mx >= areaX && mx <= areaX + areaW - 46 &&
                    my >= modY && my < modY + MODULE_H) {
                if (button == 0 && !module.getSettings().isEmpty()) {
                    expandedModules.put(module, !expandedModules.getOrDefault(module, false));
                } else if (button == 1) {
                    module.toggle();
                }
                return true;
            }

            // Settings clicks
            if (expandedModules.getOrDefault(module, false)) {
                float settingY = modY + MODULE_H + 2;
                for (Setting setting : module.getSettings()) {
                    if (mx >= areaX + 18 && mx <= areaX + areaW - 18 &&
                            my >= settingY && my < settingY + SETTING_H) {

                        if (setting instanceof BooleanSetting bs) {
                            bs.toggle();
                        } else if (setting instanceof ModeSetting ms) {
                            if (button == 0) ms.cycle();
                        } else if (setting instanceof NumberSetting ns) {
                            draggingSlider = ns;
                            sliderLeft = areaX + 24;
                            sliderWidth = areaW - 48;
                        }
                        return true;
                    }
                    settingY += SETTING_H;
                }
            }

            modY += contentH + 4;
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging) {
            winX = (float) click.x() - dragOffX;
            winY = (float) click.y() - dragOffY;
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        draggingSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (currentTab == Tab.THEMES) {
            themeScrollOffset += (float) (vAmount * 25);
            if (themeScrollOffset > 0) themeScrollOffset = 0;
        } else {
            scrollOffset += (float) (vAmount * 25);
            if (scrollOffset > 0) scrollOffset = 0;
        }
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        ChaosClient.getInstance().getConfigManager().save();
        super.close();
    }

    // ─── Static accessors for theme settings ──────────────────
    public static float getGradientSpeed() { return gradientSpeed; }
    public static float getPulseFrequency() { return pulseFrequency; }
    public static float getGlowIntensity() { return glowIntensity; }
    public static boolean isDarkMode() { return darkMode; }
    public static boolean isShadowEnabled() { return shadowEnabled; }
    public static boolean isGlowEnabled() { return glowEnabled; }
}
