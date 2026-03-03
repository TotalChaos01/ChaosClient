package me.totalchaos01.chaosclient.setting;

import java.util.function.Supplier;

/**
 * Base class for module settings.
 */
public abstract class Setting {
    private final String name;
    private final String description;
    private Supplier<Boolean> visibility;

    public Setting(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Setting(String name) {
        this(name, "");
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sets a visibility predicate. When set, the setting is only shown
     * in the GUI if the supplier returns true.
     */
    public Setting setVisibility(Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }

    /**
     * Returns true if this setting should be visible in the GUI.
     */
    public boolean isVisible() {
        return visibility == null || visibility.get();
    }
}

