package me.totalchaos01.chaosclient.ui.clickgui;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.setting.Setting;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Click GUI for ChaosClient module management.
 * Press Right Shift to open.
 */
public class ClickGuiScreen extends Screen {

    private static final int PANEL_WIDTH = 110;
    private static final int PANEL_HEADER = 22;
    private static final int MODULE_HEIGHT = 16;
    private static final int SETTING_HEIGHT = 14;
    private static final int GAP = 6;

    // Panel positions (draggable)
    private final Map<Category, int[]> panelPositions = new HashMap<>();
    private final Map<Module, Boolean> expandedModules = new HashMap<>();

    // Drag state
    private Category draggingPanel = null;
    private int dragOffsetX, dragOffsetY;

    // Scroll
    private final Map<Category, Integer> scrollOffsets = new HashMap<>();

    public ClickGuiScreen() {
        super(Text.literal("ChaosClient"));
    }

    @Override
    protected void init() {
        super.init();
        // Initialize panel positions if not set
        Category[] cats = Category.values();
        int startX = 20;
        for (Category cat : cats) {
            if (!panelPositions.containsKey(cat)) {
                panelPositions.put(cat, new int[]{startX, 30});
                startX += PANEL_WIDTH + GAP;
            }
            scrollOffsets.putIfAbsent(cat, 0);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Semi-transparent background
        ctx.fill(0, 0, width, height, 0x80000000);

        for (Category category : Category.values()) {
            renderPanel(ctx, category, mouseX, mouseY);
        }
    }

    private void renderPanel(DrawContext ctx, Category category, int mouseX, int mouseY) {
        int[] pos = panelPositions.get(category);
        int x = pos[0], y = pos[1];

        List<Module> modules = ChaosClient.getInstance().getModuleManager().getModulesByCategory(category);
        int totalHeight = PANEL_HEADER + modules.size() * MODULE_HEIGHT;

        // Count expanded settings
        for (Module m : modules) {
            if (expandedModules.getOrDefault(m, false)) {
                totalHeight += m.getSettings().size() * SETTING_HEIGHT;
            }
        }

        // Panel background
        ctx.fill(x, y, x + PANEL_WIDTH, y + totalHeight, 0xE0161622);
        // Border
        ctx.fill(x, y, x + PANEL_WIDTH, y + 1, 0xFF9333EA);
        ctx.fill(x, y + totalHeight - 1, x + PANEL_WIDTH, y + totalHeight, 0xFF2a2a3a);
        ctx.fill(x, y, x + 1, y + totalHeight, 0xFF2a2a3a);
        ctx.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + totalHeight, 0xFF2a2a3a);

        // Header
        ctx.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEADER, 0xFF9333EA);
        drawCentered(ctx, category.getDisplayName(), x + PANEL_WIDTH / 2, y + 7, 0xFFFFFFFF);

        // Modules
        int my = y + PANEL_HEADER;
        for (Module module : modules) {
            boolean hovered = mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= my && mouseY < my + MODULE_HEIGHT;

            // Module background
            int bgColor = module.isEnabled() ? 0xFF6D28D9 : (hovered ? 0xFF252535 : 0xFF1a1a28);
            ctx.fill(x + 1, my, x + PANEL_WIDTH - 1, my + MODULE_HEIGHT, bgColor);

            // Module name
            int textColor = module.isEnabled() ? 0xFFFFFFFF : (hovered ? 0xFFccccdd : 0xFFaaaabc);
            ctx.drawTextWithShadow(textRenderer, module.getName(), x + 5, my + 4, textColor);

            // Keybind indicator
            if (module.getKeyBind() != 0) {
                String key = org.lwjgl.glfw.GLFW.glfwGetKeyName(module.getKeyBind(), 0);
                if (key != null) {
                    int kw = textRenderer.getWidth("[" + key + "]");
                    ctx.drawTextWithShadow(textRenderer, "§8[" + key + "]", x + PANEL_WIDTH - kw - 6, my + 4, 0xFF666677);
                }
            }

            // Settings indicator
            if (!module.getSettings().isEmpty()) {
                boolean expanded = expandedModules.getOrDefault(module, false);
                String arrow = expanded ? "▾" : "▸";
                ctx.drawTextWithShadow(textRenderer, arrow, x + PANEL_WIDTH - 10, my + 4, 0xFF888899);
            }

            my += MODULE_HEIGHT;

            // Render settings if expanded
            if (expandedModules.getOrDefault(module, false)) {
                for (Setting setting : module.getSettings()) {
                    boolean settingHovered = mouseX >= x && mouseX <= x + PANEL_WIDTH && mouseY >= my && mouseY < my + SETTING_HEIGHT;
                    ctx.fill(x + 1, my, x + PANEL_WIDTH - 1, my + SETTING_HEIGHT, 0xFF111120);

                    if (setting instanceof BooleanSetting bs) {
                        int sc = bs.isEnabled() ? 0xFF22c55e : 0xFF666677;
                        String state = bs.isEnabled() ? "§a✓" : "§7✗";
                        ctx.drawTextWithShadow(textRenderer, "  " + setting.getName(), x + 5, my + 3, settingHovered ? 0xFFddddee : 0xFF999aab);
                        ctx.drawTextWithShadow(textRenderer, state, x + PANEL_WIDTH - 14, my + 3, sc);
                    } else if (setting instanceof NumberSetting ns) {
                        String val = ns.getIncrement() >= 1 ? String.valueOf((int) ns.getValue()) : String.format("%.1f", ns.getValue());
                        ctx.drawTextWithShadow(textRenderer, "  " + setting.getName(), x + 5, my + 3, 0xFF999aab);
                        int vw = textRenderer.getWidth(val);
                        ctx.drawTextWithShadow(textRenderer, "§b" + val, x + PANEL_WIDTH - vw - 6, my + 3, 0xFF3b82f6);
                    } else if (setting instanceof ModeSetting ms) {
                        ctx.drawTextWithShadow(textRenderer, "  " + setting.getName(), x + 5, my + 3, 0xFF999aab);
                        int mw = textRenderer.getWidth(ms.getMode());
                        ctx.drawTextWithShadow(textRenderer, "§d" + ms.getMode(), x + PANEL_WIDTH - mw - 6, my + 3, 0xFFa855f7);
                    }

                    my += SETTING_HEIGHT;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mx = (int) click.x(), my = (int) click.y();
        int button = click.button();

        for (Category category : Category.values()) {
            int[] pos = panelPositions.get(category);
            int x = pos[0], y = pos[1];

            // Check header click (drag)
            if (mx >= x && mx <= x + PANEL_WIDTH && my >= y && my < y + PANEL_HEADER) {
                draggingPanel = category;
                dragOffsetX = mx - x;
                dragOffsetY = my - y;
                return true;
            }

            // Check module clicks
            List<Module> modules = ChaosClient.getInstance().getModuleManager().getModulesByCategory(category);
            int cy = y + PANEL_HEADER;
            for (Module module : modules) {
                if (mx >= x && mx <= x + PANEL_WIDTH && my >= cy && my < cy + MODULE_HEIGHT) {
                    if (button == 0) {
                        // Left click — toggle
                        module.toggle();
                    } else if (button == 1) {
                        // Right click — expand settings
                        if (!module.getSettings().isEmpty()) {
                            expandedModules.put(module, !expandedModules.getOrDefault(module, false));
                        }
                    }
                    return true;
                }
                cy += MODULE_HEIGHT;

                // Skip settings area
                if (expandedModules.getOrDefault(module, false)) {
                    for (Setting setting : module.getSettings()) {
                        if (mx >= x && mx <= x + PANEL_WIDTH && my >= cy && my < cy + SETTING_HEIGHT) {
                            if (setting instanceof BooleanSetting bs) {
                                bs.toggle();
                            } else if (setting instanceof ModeSetting ms) {
                                ms.cycle();
                            } else if (setting instanceof NumberSetting ns) {
                                if (button == 0) {
                                    ns.setValue(ns.getValue() + ns.getIncrement());
                                } else if (button == 1) {
                                    ns.setValue(ns.getValue() - ns.getIncrement());
                                }
                            }
                            return true;
                        }
                        cy += SETTING_HEIGHT;
                    }
                }
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingPanel != null) {
            int[] pos = panelPositions.get(draggingPanel);
            pos[0] = (int) click.x() - dragOffsetX;
            pos[1] = (int) click.y() - dragOffsetY;
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPanel = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        // Save config when closing GUI
        ChaosClient.getInstance().getConfigManager().save();
        super.close();
    }

    private void drawCentered(DrawContext ctx, String text, int centerX, int y, int color) {
        int w = textRenderer.getWidth(text);
        ctx.drawTextWithShadow(textRenderer, text, centerX - w / 2, y, color);
    }
}
