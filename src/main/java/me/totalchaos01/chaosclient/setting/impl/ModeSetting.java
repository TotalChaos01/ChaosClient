package me.totalchaos01.chaosclient.setting.impl;

import me.totalchaos01.chaosclient.setting.Setting;

public class ModeSetting extends Setting {
    private String mode;
    private final String[] modes;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name);
        this.mode = defaultMode;
        this.modes = modes;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String[] getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return this.mode.equalsIgnoreCase(mode);
    }

    public void cycle() {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(mode)) {
                mode = modes[(i + 1) % modes.length];
                return;
            }
        }
    }
}

