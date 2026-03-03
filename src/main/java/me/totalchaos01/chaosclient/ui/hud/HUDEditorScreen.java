package me.totalchaos01.chaosclient.ui.hud;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

/**
 * HUD Editor screen — drag-and-drop HUD elements to reposition them.
 * Shows element boundaries with labels, grid guides, and snap-to-edge.
 * Press ESC to save and exit.
 */
public class HUDEditorScreen extends Screen {

    private HUDElement dragging = null;
    private float dragOffX, dragOffY;

    public HUDEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    protected void init() {
        super.init();
        HUDManager mgr = ChaosClient.getInstance().getHudManager();
        if (mgr != null) mgr.init();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Don't dim background — show actual game HUD underneath
        HUDManager mgr = ChaosClient.getInstance().getHudManager();
        if (mgr == null) return;

        Color theme = ThemeUtil.getThemeColor(0, ThemeType.GENERAL, 1);
        int accent = ColorUtil.toARGB(theme);

        // Hint bar at top
        String hint = "\u00A77\u041F\u0435\u0440\u0435\u0442\u0430\u0441\u043A\u0438\u0432\u0430\u0439\u0442\u0435 \u044D\u043B\u0435\u043C\u0435\u043D\u0442\u044B \u043C\u044B\u0448\u044C\u044E  \u00A78|  \u00A77ESC \u2014 \u0441\u043E\u0445\u0440\u0430\u043D\u0438\u0442\u044C";
        int hintW = client.textRenderer.getWidth(hint);
        RenderUtil.roundedRectSimple(ctx, width / 2 - hintW / 2 - 8, 6, hintW + 16, 16, 8, 0xCC101018);
        ctx.drawTextWithShadow(client.textRenderer, hint, width / 2 - hintW / 2, 10, 0xFFCCCCDD);

        // Subtle grid lines
        for (int gx = 0; gx < width; gx += 50) {
            ctx.fill(gx, 0, gx + 1, height, 0x08FFFFFF);
        }
        for (int gy = 0; gy < height; gy += 50) {
            ctx.fill(0, gy, width, gy + 1, 0x08FFFFFF);
        }

        // Center crosshair guides
        ctx.fill(width / 2, 0, width / 2 + 1, height, 0x15FFFFFF);
        ctx.fill(0, height / 2, width, height / 2 + 1, 0x15FFFFFF);

        // Draw each HUD element as a draggable rectangle
        for (HUDElement element : mgr.getAll()) {
            float ex = element.getX();
            float ey = element.getY();
            float ew = element.getWidth();
            float eh = element.getHeight();

            boolean hovered = mouseX >= ex && mouseX <= ex + ew && mouseY >= ey && mouseY <= ey + eh;
            boolean isDragging = element == dragging;

            // Element background
            int bg = isDragging ? 0x30FFFFFF : (hovered ? 0x18FFFFFF : 0x0CFFFFFF);
            RenderUtil.roundedRectSimple(ctx, (int) ex, (int) ey, (int) ew, (int) eh, 4, bg);

            // Border
            int borderColor = isDragging ? accent : (hovered ? 0x70FFFFFF : 0x30FFFFFF);
            RenderUtil.roundedRectOutline(ctx, (int) ex, (int) ey, (int) ew, (int) eh, 4, 1, borderColor);

            // Label centered in element
            String label = element.getDisplayName();
            int labelW = client.textRenderer.getWidth(label);
            int labelX = (int) (ex + ew / 2 - labelW / 2);
            int labelY = (int) (ey + eh / 2 - 4);
            ctx.drawTextWithShadow(client.textRenderer, label, labelX, labelY,
                    isDragging ? 0xFFFFFFFF : 0xFFBBBBCC);

            // Position indicator when hovered/dragging
            if (isDragging || hovered) {
                String pos = String.format("(%d, %d)", (int) ex, (int) ey);
                ctx.drawTextWithShadow(client.textRenderer, "\u00A78" + pos,
                        (int) ex, (int) (ey - 10), 0xFF666677);
            }
        }

        // Handle dragging — update position
        if (dragging != null) {
            float newX = mouseX - dragOffX;
            float newY = mouseY - dragOffY;

            // Snap to edges and center
            newX = snapX(newX, dragging.getWidth());
            newY = snapY(newY, dragging.getHeight());

            // Clamp to screen bounds
            newX = Math.max(0, Math.min(newX, width - dragging.getWidth()));
            newY = Math.max(0, Math.min(newY, height - dragging.getHeight()));

            dragging.setX(newX);
            dragging.setY(newY);
        }
    }

    private float snapX(float x, float w) {
        int threshold = 6;
        // Left edge
        if (Math.abs(x) < threshold) return 0;
        // Center
        if (Math.abs(x + w / 2 - width / 2f) < threshold) return width / 2f - w / 2;
        // Right edge
        if (Math.abs(x + w - width) < threshold) return width - w;
        return x;
    }

    private float snapY(float y, float h) {
        int threshold = 6;
        if (Math.abs(y) < threshold) return 0;
        if (Math.abs(y + h / 2 - height / 2f) < threshold) return height / 2f - h / 2;
        if (Math.abs(y + h - height) < threshold) return height - h;
        return y;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int mx = (int) click.x(), my = (int) click.y();
        HUDManager mgr = ChaosClient.getInstance().getHudManager();
        if (mgr == null) return super.mouseClicked(click, bl);

        // Find topmost element under cursor (last match = top Z-order)
        HUDElement found = null;
        for (HUDElement e : mgr.getAll()) {
            if (e.contains(mx, my)) {
                found = e;
            }
        }

        if (found != null && click.button() == 0) {
            dragging = found;
            dragOffX = mx - found.getX();
            dragOffY = my - found.getY();
            return true;
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging != null) {
            float newX = (float) click.x() - dragOffX;
            float newY = (float) click.y() - dragOffY;

            newX = snapX(newX, dragging.getWidth());
            newY = snapY(newY, dragging.getHeight());

            newX = Math.max(0, Math.min(newX, width - dragging.getWidth()));
            newY = Math.max(0, Math.min(newY, height - dragging.getHeight()));

            dragging.setX(newX);
            dragging.setY(newY);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public void close() {
        dragging = null;
        // Save positions on close
        ChaosClient.getInstance().getConfigManager().save();
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
