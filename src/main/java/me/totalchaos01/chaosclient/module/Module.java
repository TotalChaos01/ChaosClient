package me.totalchaos01.chaosclient.module;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for all modules in ChaosClient.
 * Inspired by Rise architecture but completely rewritten for Fabric 1.21.1.
 */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keyBind;
    private final boolean hidden;
    private final List<Setting> settings = new ArrayList<>();

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public Module() {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        if (info == null) {
            throw new RuntimeException("Module " + getClass().getSimpleName() + " is missing @ModuleInfo annotation!");
        }
        this.name = info.name();
        this.description = info.description();
        this.category = info.category();
        this.keyBind = info.keyBind();
        this.hidden = info.hidden();
    }

    /**
     * Called when the module is enabled.
     */
    protected void onEnable() {
    }

    /**
     * Called when the module is disabled.
     */
    protected void onDisable() {
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            ChaosClient.getInstance().getEventBus().register(this);
            onEnable();
        } else {
            ChaosClient.getInstance().getEventBus().unregister(this);
            onDisable();
        }
    }

    public void setEnabled(boolean state) {
        if (this.enabled != state) {
            toggle();
        }
    }

    protected void addSettings(Setting... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public boolean isHidden() {
        return hidden;
    }

    public List<Setting> getSettings() {
        return settings;
    }

    /**
     * Returns display info (e.g. mode name) shown next to module name in HUD.
     */
    public String getSuffix() {
        return null;
    }
}

