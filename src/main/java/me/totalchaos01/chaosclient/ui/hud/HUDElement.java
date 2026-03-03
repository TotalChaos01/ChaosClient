package me.totalchaos01.chaosclient.ui.hud;

/**
 * Represents a single draggable HUD element with position and size data.
 * Used by HUDManager and HUDEditorScreen for drag-and-drop positioning.
 */
public class HUDElement {

    private final String id;
    private final String displayName;
    private float x, y;
    private float width, height;
    private boolean visible;

    public HUDElement(String id, String displayName, float x, float y, float width, float height) {
        this.id = id;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = true;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }

    public float getX() { return x; }
    public float getY() { return y; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }

    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public void setWidth(float w) { this.width = w; }
    public void setHeight(float h) { this.height = h; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    /**
     * Check if a screen point is within this element's bounds.
     */
    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }
}
