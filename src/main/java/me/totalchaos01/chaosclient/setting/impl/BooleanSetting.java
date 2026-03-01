package me.totalchaos01.chaosclient.setting.impl;

import me.totalchaos01.chaosclient.setting.Setting;

public class BooleanSetting extends Setting {
    private boolean enabled;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.enabled = defaultValue;
    }

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description);
        this.enabled = defaultValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}

