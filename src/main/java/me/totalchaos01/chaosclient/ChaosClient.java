package me.totalchaos01.chaosclient;

import me.totalchaos01.chaosclient.bootstrap.ChaosAgent;
import me.totalchaos01.chaosclient.command.CommandManager;
import me.totalchaos01.chaosclient.config.ConfigManager;
import me.totalchaos01.chaosclient.event.EventBus;
import me.totalchaos01.chaosclient.font.ChaosFont;
import me.totalchaos01.chaosclient.module.ModuleManager;
import me.totalchaos01.chaosclient.notification.NotificationManager;
import me.totalchaos01.chaosclient.ui.hud.HUDManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChaosClient — встроенный клиент для Minecraft 1.21.11.
 *
 * Архитектура загрузки:
 * 1. PreLaunch: ChaosPreLaunch инициализируется ДО загрузки Minecraft классов
 * 2. Agent (опционально): ChaosAgent загружается через -javaagent для глубокой интеграции
 * 3. Client Init: ChaosClient.onInitializeClient() — основная инициализация
 *
 * Режимы загрузки:
 * - Classpath (встроенный): jar в libraries/, Fabric обнаруживает как builtin
 * - Java Agent: -javaagent:ChaosClient.jar (дополнительные возможности инструментации)
 * - Fabric Mod (обратная совместимость): jar в mods/
 *
 * Access Widener открывает прямой доступ к полям Minecraft:
 * - MinecraftClient.session, itemUseCooldown, doAttack()
 * - ClientPlayerEntity.lastYaw, lastPitch
 * - ClientConnection.channel
 * - Input.movementVector, playerInput
 * - SimpleOption.value (для Fullbright gamma > 1.0)
 * - И многое другое — см. chaosclient.accesswidener
 */
public class ChaosClient implements ClientModInitializer {

    public static final String CLIENT_NAME = "ChaosClient";
    public static final String CLIENT_VERSION = "1.3.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);

    private static ChaosClient INSTANCE;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private NotificationManager notificationManager;
    private HUDManager hudManager;

    // Integration mode
    private boolean agentMode;
    private boolean classpathMode;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        // Detect loading mode
        agentMode = ChaosAgent.isAgentLoaded();
        classpathMode = !isLoadedFromModsFolder();

        String mode = agentMode ? "Agent + Fabric" : (classpathMode ? "Classpath (встроенный)" : "Fabric Mod");
        LOGGER.info("Starting {} v{} [{}]", CLIENT_NAME, CLIENT_VERSION, mode);

        // Initialize core systems
        eventBus = new EventBus();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        notificationManager = new NotificationManager();
        hudManager = new HUDManager();

        // Register all modules & commands
        moduleManager.init();
        commandManager.init();

        // Initialize independent font system (captures TextRenderer before resource packs)
        ChaosFont.init();

        // Load saved config
        configManager.load();

        // Enable default modules on first launch
        if (!configManager.hasConfig()) {
            enableDefaults();
        }

        if (agentMode) {
            LOGGER.info("Agent instrumentation available — class retransformation enabled.");
        }

        LOGGER.info("{} initialized successfully!", CLIENT_NAME);
    }

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

    /**
     * Detect if loaded from the mods/ folder vs classpath/libraries.
     */
    private boolean isLoadedFromModsFolder() {
        try {
            String location = ChaosClient.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
            return location.contains("/mods/");
        } catch (Exception e) {
            return true;
        }
    }

    // === Accessors ===
    public static ChaosClient getInstance() { return INSTANCE; }
    public static MinecraftClient mc() { return MinecraftClient.getInstance(); }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public HUDManager getHudManager() { return hudManager; }
    public boolean isAgentMode() { return agentMode; }
    public boolean isClasspathMode() { return classpathMode; }
}
