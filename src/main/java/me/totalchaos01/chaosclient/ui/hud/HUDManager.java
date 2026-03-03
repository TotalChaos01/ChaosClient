package me.totalchaos01.chaosclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages all HUD element positions for the drag-and-drop editor.
 * Stores absolute screen positions that are read by rendering code.
 * Supports JSON serialization for config persistence.
 */
public class HUDManager {

    private final Map<String, HUDElement> elements = new LinkedHashMap<>();
    private boolean initialized = false;

    /**
     * Initialize default element positions based on current screen size.
     * Safe to call multiple times — only initializes once.
     */
    public void init() {
        if (initialized) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = 854, h = 480; // fallback
        if (mc.getWindow() != null) {
            w = mc.getWindow().getScaledWidth();
            h = mc.getWindow().getScaledHeight();
        }

        // Default positions — these match the hardcoded positions in HUD.java and other modules
        register(new HUDElement("watermark", "\u0412\u0430\u0442\u0435\u0440\u043C\u0430\u0440\u043A", 0, 0, 90, 28));
        register(new HUDElement("arraylist", "\u041C\u0430\u0441\u0441\u0438\u0432 \u043C\u043E\u0434\u0443\u043B\u0435\u0439", w - 120, 2, 116, 150));
        register(new HUDElement("coordinates", "\u041A\u043E\u043E\u0440\u0434\u0438\u043D\u0430\u0442\u044B", 4, h - 16, 180, 12));
        register(new HUDElement("fps", "FPS", 4, h - 26, 80, 12));
        register(new HUDElement("bps", "BPS", 4, h - 36, 80, 12));
        register(new HUDElement("targethud", "TargetHUD", w / 2 + 12, h / 2 + 12, 160, 52));
        register(new HUDElement("notifications", "\u0423\u0432\u0435\u0434\u043E\u043C\u043B\u0435\u043D\u0438\u044F", w - 210, h - 120, 200, 100));

        initialized = true;
    }

    private void register(HUDElement element) {
        elements.put(element.getId(), element);
    }

    /**
     * Get an element by its ID. Returns null if not found.
     */
    public HUDElement get(String id) {
        return elements.get(id);
    }

    /**
     * Get all registered elements.
     */
    public Collection<HUDElement> getAll() {
        return elements.values();
    }

    public float getX(String id) {
        HUDElement e = elements.get(id);
        return e != null ? e.getX() : 0;
    }

    public float getY(String id) {
        HUDElement e = elements.get(id);
        return e != null ? e.getY() : 0;
    }

    // --- JSON serialization for config ---

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        for (HUDElement e : elements.values()) {
            JsonObject el = new JsonObject();
            el.addProperty("x", e.getX());
            el.addProperty("y", e.getY());
            el.addProperty("visible", e.isVisible());
            obj.add(e.getId(), el);
        }
        return obj;
    }

    public void fromJson(JsonObject obj) {
        if (obj == null) return;
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            HUDElement e = elements.get(entry.getKey());
            if (e != null && entry.getValue().isJsonObject()) {
                JsonObject el = entry.getValue().getAsJsonObject();
                if (el.has("x")) e.setX(el.get("x").getAsFloat());
                if (el.has("y")) e.setY(el.get("y").getAsFloat());
                if (el.has("visible")) e.setVisible(el.get("visible").getAsBoolean());
            }
        }
    }
}
