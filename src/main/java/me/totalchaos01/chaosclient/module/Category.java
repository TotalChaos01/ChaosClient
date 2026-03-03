package me.totalchaos01.chaosclient.module;

/**
 * Module categories.
 */
public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    LEGIT("Legit"),
    EXPLOITS("Exploits");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

