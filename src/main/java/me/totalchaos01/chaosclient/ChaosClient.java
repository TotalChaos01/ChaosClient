package me.totalchaos01.chaosclient;

import me.totalchaos01.chaosclient.command.CommandManager;
import me.totalchaos01.chaosclient.config.ConfigManager;
import me.totalchaos01.chaosclient.event.EventBus;
import me.totalchaos01.chaosclient.module.ModuleManager;
import me.totalchaos01.chaosclient.notification.NotificationManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for ChaosClient — a universal Minecraft client.
 * No license checks, no telemetry, no spyware. Clean and open.
 */
public class ChaosClient implements ClientModInitializer {

    public static final String CLIENT_NAME = "ChaosClient";
    public static final String CLIENT_VERSION = "1.1.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);

    private static ChaosClient INSTANCE;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private NotificationManager notificationManager;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("Starting {} v{}", CLIENT_NAME, CLIENT_VERSION);

        // Initialize core systems
        eventBus = new EventBus();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        notificationManager = new NotificationManager();

        // Register all modules
        moduleManager.init();
        commandManager.init();

        // Load saved config
        configManager.load();

        // Enable default modules on first launch (no config file)
        if (!configManager.hasConfig()) {
            enableDefaults();
        }

        LOGGER.info("{} initialized successfully!", CLIENT_NAME);
    }

    /**
     * Enable modules that should be active on first launch.
     */
    private void enableDefaults() {
        String[] defaults = {"HUD", "Fullbright", "Sprint"};
        for (String name : defaults) {
            var module = moduleManager.getModule(name);
            if (module != null && !module.isEnabled()) {
                module.toggle();
            }
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down {}...", CLIENT_NAME);
        configManager.save();
    }

    public static ChaosClient getInstance() {
        return INSTANCE;
    }

    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}

