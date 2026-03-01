package me.totalchaos01.chaosclient.setting.impl;

import me.totalchaos01.chaosclient.setting.Setting;

public class NumberSetting extends Setting {
    private double value;
    private final double min;
    private final double max;
    private final double increment;

    public NumberSetting(String name, double defaultValue, double min, double max, double increment) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double increment) {
        super(name, description);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        double rounded = Math.round(value / increment) * increment;
        this.value = Math.max(min, Math.min(max, rounded));
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }
}

