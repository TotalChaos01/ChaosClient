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
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redesigned ClickGUI — smooth rounded UI with theme panel as sidebar tab,
 * player profile in bottom-left, middle-click keybind, right-click settings.
 * All UI in Russian.
 */
public class ClickGuiScreen extends Screen {

    // --- Layout ---
    private static final int CAT_WIDTH = 110;
    private static final int CAT_HEIGHT = 26;
    private static final int HEADER_H = 30;
    private static final int MODULE_H = 28;
    private static final int SETTING_H = 22;
    private static final int PAD = 8;
    private static final int PROFILE_H = 44;

    // --- Window ---
    private float winX = -1, winY = -1;
    private float winW = 540, winH = 400;

    // --- Animations ---
    private float renderSelectY = 0;
    private float scrollOffset = 0;
    private float themeScrollY = 0;
    private final Animate openAnim = new Animate(0.85, 0.08);
    private final Map<String, Animate> toggleAnims = new HashMap<>();
    private final Map<String, Animate> sliderAnims = new HashMap<>();

    // --- State ---
    private enum SidebarTab { MODULES, THEMES }
    private SidebarTab activeTab = SidebarTab.MODULES;
    private Category selectedCat = Category.COMBAT;
    private final Map<Module, Boolean> expandedModules = new HashMap<>();
    private boolean dragging = false;
    private float dragOffX, dragOffY;
    private NumberSetting draggingSlider = null;
    private long openTime;
    private Module bindingModule = null;

    // --- Colors ---
    private int colorBg, colorSidebar, colorHeader, colorAccent, colorAccent2;

    // --- Category icons ---
    private static final Map<Category, String> ICONS = Map.of(
            Category.COMBAT, "\u2694",
            Category.MOVEMENT, "\u27A4",
            Category.PLAYER, "\u263A",
            Category.RENDER, "\u25C9",
            Category.GHOST, "\uD83D\uDC7B",
            Category.OTHER, "\u2699"
    );

    // --- Russian category names ---
    private static final Map<Category, String> RU_NAMES = Map.of(
            Category.COMBAT, "\u0411\u043E\u0439",
            Category.MOVEMENT, "\u0414\u0432\u0438\u0436\u0435\u043D\u0438\u0435",
            Category.PLAYER, "\u0418\u0433\u0440\u043E\u043A",
            Category.RENDER, "\u0412\u0438\u0437\u0443\u0430\u043B",
            Category.GHOST, "\u0413\u043E\u0441\u0442",
            Category.OTHER, "\u0414\u0440\u0443\u0433\u043E\u0435"
    );

    // --- Persisted theme settings ---
    private static float gradientSpeed = 1.0f;
    private static float glowIntensity = 1.0f;
    private static boolean darkMode = true;
    private static boolean shadowEnabled = true;
    private static boolean glowEnabled = true;

    public ClickGuiScreen() {
        super(Text.literal("ChaosClient"));
    }

    @Override
    protected void init() {
        super.init();
        openAnim.setValue(0.85);
        openAnim.setTarget(1.0);
        dragging = false;
        draggingSlider = null;
        bindingModule = null;
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

        if (darkMode) {
            colorBg = 0xFF141620;
            colorSidebar = 0xFF0F1118;
            colorHeader = 0xFF141620;
        } else {
            colorBg = 0xFF24262E;
            colorSidebar = 0xFF1E2028;
            colorHeader = 0xFF24262E;
        }
    }

    // ============================================================
    //  RENDER
    // ============================================================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        updateColors();
        openAnim.update();
        float scale = (float) openAnim.getValue();

        int overlayAlpha = (int) (0x70 * Math.min(1.0, (System.currentTimeMillis() - openTime) / 300.0));
        ctx.fill(0, 0, this.width, this.height, (overlayAlpha << 24));

        ctx.getMatrices().pushMatrix();
        float cx = winX + winW / 2f, cy = winY + winH / 2f;
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(-cx, -cy);

        if (shadowEnabled) {
            RenderUtil.shadow(ctx, winX, winY, winW, winH, 14, 0xFF000000);
        }

        if (glowEnabled) {
            Color gc = new Color((colorAccent >> 16) & 0xFF, (colorAccent >> 8) & 0xFF,
                    colorAccent & 0xFF, 35);
            RenderUtil.glow(ctx, winX, winY, winW, winH, 14, gc, 3);
        }

        // Sidebar background
        RenderUtil.roundedRectSimple(ctx, (int) winX, (int) winY, CAT_WIDTH, (int) winH, 14, colorSidebar);
        ctx.fill((int) (winX + CAT_WIDTH - 14), (int) winY, (int) (winX + CAT_WIDTH), (int) (winY + winH), colorSidebar);

        // Module area background
        RenderUtil.roundedRectSimple(ctx, (int) (winX + CAT_WIDTH), (int) winY,
                (int) (winW - CAT_WIDTH), (int) winH, 14, colorBg);
        ctx.fill((int) (winX + CAT_WIDTH), (int) winY,
                (int) (winX + CAT_WIDTH + 14), (int) (winY + winH), colorBg);

        // Header background
        RenderUtil.roundedRectSimple(ctx, (int) (winX + CAT_WIDTH), (int) winY,
                (int) (winW - CAT_WIDTH), HEADER_H, 14, colorHeader);
        ctx.fill((int) (winX + CAT_WIDTH), (int) (winY + HEADER_H - 6),
                (int) (winX + winW), (int) (winY + HEADER_H), colorHeader);

        // Header accent line
        RenderUtil.gradientLine(ctx, (int) (winX + CAT_WIDTH), (int) (winY + HEADER_H - 1),
                (int) (winW - CAT_WIDTH), 1, colorAccent, colorAccent2);

        // Client name + version in header
        RenderUtil.drawGradientText(ctx, ChaosClient.CLIENT_NAME,
                (int) (winX + CAT_WIDTH + 14), (int) (winY + 10), 0, 1.5f);
        int nameW = client.textRenderer.getWidth(ChaosClient.CLIENT_NAME);
        ctx.drawTextWithShadow(client.textRenderer, " v" + ChaosClient.CLIENT_VERSION,
                (int) (winX + CAT_WIDTH + 16 + nameW), (int) (winY + 10), 0xFF555566);

        // Keybind prompt
        if (bindingModule != null) {
            String prompt = "\u041D\u0430\u0436\u043C\u0438\u0442\u0435 \u043A\u043B\u0430\u0432\u0438\u0448\u0443: " + bindingModule.getName();
            int pw = client.textRenderer.getWidth(prompt);
            ctx.drawTextWithShadow(client.textRenderer, prompt,
                    (int) (winX + winW - pw - 12), (int) (winY + 10), 0xFFFFAA44);
        }

        // Sidebar
        renderSidebar(ctx, mouseX, mouseY);

        // Player profile
        renderPlayerProfile(ctx);

        // Content area (clipped)
        int contentX = (int) (winX + CAT_WIDTH + 1);
        int contentY = (int) (winY + HEADER_H);
        int contentW = (int) (winW - CAT_WIDTH - 1);
        int contentH = (int) (winH - HEADER_H);

        ctx.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        if (activeTab == SidebarTab.MODULES) {
            renderModules(ctx, mouseX, mouseY, contentX, contentY, contentW);
        } else {
            renderThemesPanel(ctx, mouseX, mouseY, contentX, contentY, contentW, contentH);
        }
        ctx.disableScissor();

        ctx.getMatrices().popMatrix();
    }

    // --- Sidebar (categories + themes tab) ---

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        Category[] cats = Category.values();
        int targetIdx = -1;
        if (activeTab == SidebarTab.MODULES) {
            for (int i = 0; i < cats.length; i++) {
                if (cats[i] == selectedCat) { targetIdx = i; break; }
            }
        }

        float selectorTargetY;
        if (targetIdx >= 0) {
            selectorTargetY = HEADER_H + CAT_HEIGHT * targetIdx + 4;
        } else {
            selectorTargetY = HEADER_H + CAT_HEIGHT * cats.length + 12;
        }
        renderSelectY = (float) Animate.lerp(renderSelectY, selectorTargetY, 0.15);

        // Selection indicator
        RenderUtil.roundedRectSimple(ctx, (int) winX + 4, (int) (winY + renderSelectY),
                CAT_WIDTH - 8, CAT_HEIGHT, 10, ColorUtil.withAlpha(colorAccent, 180));

        if (glowEnabled) {
            Color gl = new Color((colorAccent >> 16) & 0xFF, (colorAccent >> 8) & 0xFF,
                    colorAccent & 0xFF, 25);
            RenderUtil.glow(ctx, winX + 4, winY + renderSelectY, CAT_WIDTH - 8, CAT_HEIGHT, 10, gl, 2);
        }

        // Category items with Russian names
        for (int i = 0; i < cats.length; i++) {
            float catY = winY + HEADER_H + CAT_HEIGHT * i + 4;
            String icon = ICONS.getOrDefault(cats[i], "\u2022");
            String name = RU_NAMES.getOrDefault(cats[i], cats[i].getDisplayName());

            boolean hovered = mouseX >= winX && mouseX <= winX + CAT_WIDTH &&
                    mouseY >= catY && mouseY < catY + CAT_HEIGHT;
            boolean selected = activeTab == SidebarTab.MODULES && cats[i] == selectedCat;

            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFCCCCDD : 0xFF99AABB);
            ctx.drawTextWithShadow(client.textRenderer, icon, (int) (winX + 10), (int) (catY + 8), textColor);
            ctx.drawTextWithShadow(client.textRenderer, name, (int) (winX + 26), (int) (catY + 8), textColor);
        }

        // Divider
        float divY = winY + HEADER_H + CAT_HEIGHT * cats.length + 4;
        RenderUtil.gradientLine(ctx, (int) winX + 10, (int) divY, CAT_WIDTH - 20, 1, 0xFF333344, 0xFF222233);

        // Themes tab button
        float themesY = divY + 8;
        boolean themesHovered = mouseX >= winX && mouseX <= winX + CAT_WIDTH &&
                mouseY >= themesY && mouseY < themesY + CAT_HEIGHT;
        boolean themesSelected = activeTab == SidebarTab.THEMES;

        int thCol = themesSelected ? 0xFFFFFFFF : (themesHovered ? 0xFFCCCCDD : 0xFF99AABB);
        ctx.drawTextWithShadow(client.textRenderer, "\u25C9", (int) (winX + 10), (int) (themesY + 8), thCol);
        ctx.drawTextWithShadow(client.textRenderer, "\u0422\u0435\u043C\u044B", (int) (winX + 26), (int) (themesY + 8), thCol);
    }

    // --- Player profile (bottom-left) ---

    private void renderPlayerProfile(DrawContext ctx) {
        float profileY = winY + winH - PROFILE_H;

        RenderUtil.gradientLine(ctx, (int) winX + 8, (int) profileY, CAT_WIDTH - 16, 1, 0xFF333344, 0xFF222233);

        if (client.player != null) {
            String playerName = client.player.getName().getString();
            ctx.drawTextWithShadow(client.textRenderer, "\u263A",
                    (int) (winX + 10), (int) (profileY + 14), colorAccent);
            ctx.drawTextWithShadow(client.textRenderer, playerName,
                    (int) (winX + 24), (int) (profileY + 10), 0xFFEEEEFF);
            ctx.drawTextWithShadow(client.textRenderer, "\u00A77\u0412 \u0438\u0433\u0440\u0435",
                    (int) (winX + 24), (int) (profileY + 22), 0xFF667788);
        } else {
            ctx.drawTextWithShadow(client.textRenderer, "\u263A",
                    (int) (winX + 10), (int) (profileY + 14), 0xFF667788);
            ctx.drawTextWithShadow(client.textRenderer, "\u041D\u0435 \u0432 \u0438\u0433\u0440\u0435",
                    (int) (winX + 24), (int) (profileY + 14), 0xFF667788);
        }
    }

    // --- Module list ---

    private void renderModules(DrawContext ctx, int mouseX, int mouseY, int areaX, int areaY, int areaW) {
        List<Module> modules = ChaosClient.getInstance().getModuleManager().getModulesByCategory(selectedCat);
        float my = areaY + PAD + scrollOffset;

        for (int idx = 0; idx < modules.size(); idx++) {
            Module module = modules.get(idx);
            float moduleY = my;

            float contentH = MODULE_H;
            if (expandedModules.getOrDefault(module, false)) {
                contentH = MODULE_H + 6;
                for (Setting s : module.getSettings()) contentH += SETTING_H;
            }

            if (moduleY + contentH < areaY - 10 || moduleY > areaY + winH) {
                my += contentH + 5;
                continue;
            }

            boolean hovered = mouseX >= areaX + 6 && mouseX <= areaX + areaW - 6 &&
                    mouseY >= moduleY && mouseY < moduleY + contentH;
            boolean expanded = expandedModules.getOrDefault(module, false);

            if (module.isEnabled()) {
                RenderUtil.roundedRectSimple(ctx, areaX + 6, (int) moduleY,
                        areaW - 12, (int) contentH, 10, ColorUtil.withAlpha(colorAccent, 30));
                RenderUtil.roundedRectSimple(ctx, areaX + 6, (int) moduleY + 4,
                        3, MODULE_H - 8, 2, colorAccent);
            } else if (hovered) {
                RenderUtil.roundedRectSimple(ctx, areaX + 6, (int) moduleY,
                        areaW - 12, (int) contentH, 10, 0x12FFFFFF);
            }

            int textColor = module.isEnabled() ? 0xFFFFFFFF : 0xFFBBBBCC;
            ctx.drawTextWithShadow(client.textRenderer, module.getName(),
                    areaX + 18, (int) (moduleY + 9), textColor);

            if (hovered && !expanded && module.getDescription() != null) {
                ctx.drawTextWithShadow(client.textRenderer,
                        "\u00A77" + module.getDescription(),
                        areaX + 18 + client.textRenderer.getWidth(module.getName()) + 8,
                        (int) (moduleY + 9), 0xFF555566);
            }

            // Toggle switch
            String toggleKey = module.getName() + "_toggle";
            Animate toggleAnim = toggleAnims.computeIfAbsent(toggleKey,
                    k -> new Animate(module.isEnabled() ? 1 : 0, 0.12));
            toggleAnim.setTarget(module.isEnabled() ? 1 : 0);
            toggleAnim.update();
            RenderUtil.toggleSwitch(ctx, areaX + areaW - 44, (int) (moduleY + 7),
                    28, 14, (float) toggleAnim.getValue(), 0xFF3A3D47, colorAccent);

            // Expand arrow (right-click indicator)
            if (!module.getSettings().isEmpty()) {
                String arrow = expanded ? "\u25BE" : "\u25B8";
                ctx.drawTextWithShadow(client.textRenderer, arrow,
                        areaX + areaW - 56, (int) (moduleY + 9), 0xFF666677);
            }

            // Keybind display
            if (module == bindingModule) {
                ctx.drawTextWithShadow(client.textRenderer, "[\u2026]",
                        areaX + areaW - 80, (int) (moduleY + 9), 0xFFFFAA44);
            } else if (module.getKeyBind() != 0) {
                String key = GLFW.glfwGetKeyName(module.getKeyBind(), 0);
                if (key != null) {
                    String keyText = "[" + key.toUpperCase() + "]";
                    int kw = client.textRenderer.getWidth(keyText);
                    ctx.drawTextWithShadow(client.textRenderer, "\u00A78" + keyText,
                            areaX + areaW - 58 - kw, (int) (moduleY + 9), 0xFF444455);
                }
            }

            if (expanded) {
                float settingY = moduleY + MODULE_H + 3;
                for (Setting setting : module.getSettings()) {
                    renderSetting(ctx, setting, module, areaX + 22, settingY, areaW - 44, mouseX, mouseY);
                    settingY += SETTING_H;
                }
            }

            my += contentH + 5;
        }
    }

    // --- Setting rendering ---

    private void renderSetting(DrawContext ctx, Setting setting, Module module,
                               int x, float y, int width, int mouseX, int mouseY) {
        RenderUtil.roundedRectSimple(ctx, x, (int) y, width, SETTING_H - 2, 6, 0x0AFFFFFF);

        if (setting instanceof BooleanSetting bs) {
            ctx.drawTextWithShadow(client.textRenderer, setting.getName(),
                    x + 8, (int) (y + 6), 0xFFDDDDEE);

            String key = module.getName() + "_" + setting.getName();
            Animate anim = toggleAnims.computeIfAbsent(key, k -> new Animate(bs.isEnabled() ? 1 : 0, 0.12));
            anim.setTarget(bs.isEnabled() ? 1 : 0);
            anim.update();

            RenderUtil.toggleSwitch(ctx, x + width - 30, (int) (y + 4),
                    24, 12, (float) anim.getValue(), 0xFF3A3D47, colorAccent);

        } else if (setting instanceof NumberSetting ns) {
            String val = ns.getIncrement() >= 1
                    ? String.valueOf((int) ns.getValue())
                    : String.format("%.2f", ns.getValue());

            ctx.drawTextWithShadow(client.textRenderer, setting.getName(),
                    x + 8, (int) (y + 2), 0xFFDDDDEE);
            int vw = client.textRenderer.getWidth(val);
            ctx.drawTextWithShadow(client.textRenderer, val,
                    x + width - vw - 8, (int) (y + 2), colorAccent);

            float sliderY = y + 14;
            float sliderW = width - 16;
            float percent = (float) ((ns.getValue() - ns.getMin()) / (ns.getMax() - ns.getMin()));

            String sliderKey = module.getName() + "_" + setting.getName() + "_s";
            Animate sliderAnim = sliderAnims.computeIfAbsent(sliderKey, k -> new Animate(percent, 0.15));
            sliderAnim.setTarget(percent);
            sliderAnim.update();
            float animPercent = (float) sliderAnim.getValue();

            RenderUtil.roundedRectSimple(ctx, x + 8, (int) sliderY, (int) sliderW, 4, 2, 0xFF2A2D37);

            int fillW = (int) (sliderW * animPercent);
            if (fillW > 0) {
                RenderUtil.roundedRectSimple(ctx, x + 8, (int) sliderY, fillW, 4, 2, colorAccent);
            }

            int knobX = x + 8 + (int) (sliderW * animPercent) - 3;
            RenderUtil.circle(ctx, knobX + 3, sliderY + 2, 4, 0xFFFFFFFF);

            if (draggingSlider == ns) {
                float startX = x + 8;
                float mxClamped = Math.max(startX, Math.min(mouseX, startX + sliderW));
                double pct = (mxClamped - startX) / sliderW;
                double newVal = ns.getMin() + pct * (ns.getMax() - ns.getMin());
                ns.setValue(newVal);
            }

        } else if (setting instanceof ModeSetting ms) {
            ctx.drawTextWithShadow(client.textRenderer, setting.getName(),
                    x + 8, (int) (y + 6), 0xFFDDDDEE);

            String mode = ms.getMode();
            int mw = client.textRenderer.getWidth(mode);
            int pillX = x + width - mw - 16;
            int pillY = (int) (y + 3);
            RenderUtil.roundedRectSimple(ctx, pillX, pillY, mw + 12, 14, 7,
                    ColorUtil.withAlpha(colorAccent, 50));
            ctx.drawTextWithShadow(client.textRenderer, mode, pillX + 6, pillY + 3, colorAccent);
        }
    }

    // --- Themes panel (as full content area) ---

    private void renderThemesPanel(DrawContext ctx, int mouseX, int mouseY,
                                   int areaX, int areaY, int areaW, int areaH) {
        ctx.drawTextWithShadow(client.textRenderer, "\u25C9 \u0426\u0432\u0435\u0442\u043E\u0432\u044B\u0435 \u0442\u0435\u043C\u044B",
                areaX + 14, areaY + 10, 0xFFEEEEFF);
        RenderUtil.gradientLine(ctx, areaX + 12, areaY + 22, areaW - 24, 1, colorAccent, colorAccent2);

        String[] themes = ThemeUtil.getThemeNames();
        String current = ThemeUtil.getTheme();
        int itemH = 28;
        int cols = 2;
        int itemW = (areaW - 36) / cols;
        int startY = areaY + 30;

        float drawY = startY + themeScrollY;
        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            float itemX = areaX + 12 + col * (itemW + 6);
            float itemY = drawY + row * (itemH + 4);

            boolean selected = themes[i].equals(current);
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW &&
                    mouseY >= itemY && mouseY < itemY + itemH;

            int bg = selected ? ColorUtil.withAlpha(colorAccent, 100)
                    : (hovered ? 0x25FFFFFF : 0x12FFFFFF);
            RenderUtil.roundedRectSimple(ctx, (int) itemX, (int) itemY, itemW, itemH - 2, 8, bg);

            if (selected) {
                RenderUtil.roundedRectOutline(ctx, (int) itemX, (int) itemY,
                        itemW, itemH - 2, 8, 1, ColorUtil.withAlpha(colorAccent, 200));
            }

            int textColor = selected ? 0xFFFFFFFF : 0xFFAABBCC;
            ctx.drawTextWithShadow(client.textRenderer, themes[i],
                    (int) (itemX + 10), (int) (itemY + 9), textColor);
        }
    }

    // ============================================================
    //  INPUT
    // ============================================================

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mx = (int) click.x(), my = (int) click.y();
        int button = click.button();

        // Header drag
        if (mx >= winX + CAT_WIDTH && mx <= winX + winW &&
                my >= winY && my <= winY + HEADER_H) {
            dragging = true;
            dragOffX = mx - winX;
            dragOffY = my - winY;
            return true;
        }

        // Sidebar clicks
        Category[] cats = Category.values();
        if (mx >= winX && mx <= winX + CAT_WIDTH && my >= winY + HEADER_H &&
                my < winY + winH - PROFILE_H) {

            for (int i = 0; i < cats.length; i++) {
                float catY = winY + HEADER_H + CAT_HEIGHT * i + 4;
                if (my >= catY && my < catY + CAT_HEIGHT) {
                    selectedCat = cats[i];
                    activeTab = SidebarTab.MODULES;
                    scrollOffset = 0;
                    return true;
                }
            }

            float divY = winY + HEADER_H + CAT_HEIGHT * cats.length + 4;
            float themesY = divY + 8;
            if (my >= themesY && my < themesY + CAT_HEIGHT) {
                activeTab = SidebarTab.THEMES;
                themeScrollY = 0;
                return true;
            }
        }

        // Themes panel clicks
        if (activeTab == SidebarTab.THEMES) {
            return handleThemePanelClick(mx, my);
        }

        // Module clicks: left=toggle, right=expand settings, middle=keybind
        return handleModuleClicks(mx, my, button, click, bl);
    }

    private boolean handleThemePanelClick(int mx, int my) {
        int areaX = (int) (winX + CAT_WIDTH + 1);
        int areaW = (int) (winW - CAT_WIDTH - 1);
        int startY = (int) (winY + HEADER_H + 30);

        String[] themes = ThemeUtil.getThemeNames();
        int itemH = 28;
        int cols = 2;
        int itemW = (areaW - 36) / cols;

        for (int i = 0; i < themes.length; i++) {
            int col = i % cols;
            int row = i / cols;
            float itemX = areaX + 12 + col * (itemW + 6);
            float itemY = startY + themeScrollY + row * (itemH + 4);

            if (mx >= itemX && mx <= itemX + itemW &&
                    my >= itemY && my < itemY + itemH) {
                ThemeUtil.setTheme(themes[i]);
                return true;
            }
        }
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
                contentH = MODULE_H + 6;
                for (Setting s : module.getSettings()) contentH += SETTING_H;
            }

            if (mx >= areaX + 6 && mx <= areaX + areaW - 6 &&
                    my >= modY && my < modY + MODULE_H) {

                if (button == 0) {
                    // Left click -> toggle module
                    module.toggle();
                    return true;
                } else if (button == 1) {
                    // Right click -> expand/collapse settings
                    if (!module.getSettings().isEmpty()) {
                        expandedModules.put(module, !expandedModules.getOrDefault(module, false));
                    }
                    return true;
                } else if (button == 2) {
                    // Middle click -> keybind mode
                    bindingModule = module;
                    return true;
                }
            }

            // Settings clicks
            if (expandedModules.getOrDefault(module, false)) {
                float settingY = modY + MODULE_H + 3;
                for (Setting setting : module.getSettings()) {
                    if (mx >= areaX + 22 && mx <= areaX + areaW - 22 &&
                            my >= settingY && my < settingY + SETTING_H) {

                        if (setting instanceof BooleanSetting bs) {
                            bs.toggle();
                        } else if (setting instanceof ModeSetting ms) {
                            if (button == 0) ms.cycle();
                        } else if (setting instanceof NumberSetting ns) {
                            draggingSlider = ns;
                        }
                        return true;
                    }
                    settingY += SETTING_H;
                }
            }

            modY += contentH + 5;
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
        if (activeTab == SidebarTab.THEMES) {
            themeScrollY += (float) (vAmount * 25);
            if (themeScrollY > 0) themeScrollY = 0;
            return true;
        }
        scrollOffset += (float) (vAmount * 25);
        if (scrollOffset > 0) scrollOffset = 0;
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule = null;
            } else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                bindingModule.setKeyBind(0);
                bindingModule = null;
            } else {
                bindingModule.setKeyBind(keyCode);
                bindingModule = null;
            }
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        bindingModule = null;
        ChaosClient.getInstance().getConfigManager().save();
        super.close();
    }

    // --- Static accessors for theme settings (used by ConfigManager) ---
    public static float getGradientSpeed() { return gradientSpeed; }
    public static void setGradientSpeed(float v) { gradientSpeed = v; }
    public static float getGlowIntensity() { return glowIntensity; }
    public static void setGlowIntensity(float v) { glowIntensity = v; }
    public static boolean isDarkMode() { return darkMode; }
    public static void setDarkMode(boolean v) { darkMode = v; }
    public static boolean isShadowEnabled() { return shadowEnabled; }
    public static void setShadowEnabled(boolean v) { shadowEnabled = v; }
    public static boolean isGlowEnabled() { return glowEnabled; }
    public static void setGlowEnabled(boolean v) { glowEnabled = v; }
}
