package me.totalchaos01.chaosclient.command;

import java.util.Collections;
import java.util.List;

/**
 * Base class for all chat commands.
 */
public abstract class Command {

    private final String name;
    private final String description;
    private final String usage;
    private final String[] aliases;

    public Command(String name, String description, String usage, String... aliases) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.aliases = aliases;
    }

    public abstract void execute(String[] args);

    /**
     * Returns tab-completions for the given partial arguments.
     * Override in subclasses for command-specific completions.
     */
    public List<String> getCompletions(String[] args) {
        return Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String[] getAliases() {
        return aliases;
    }
}
