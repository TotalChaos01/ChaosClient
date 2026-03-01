package me.totalchaos01.chaosclient.setting;

/**
 * Base class for module settings.
 */
public abstract class Setting {
    private final String name;
    private final String description;

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
}

