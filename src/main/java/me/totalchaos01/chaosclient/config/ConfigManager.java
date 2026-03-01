package me.totalchaos01.chaosclient.config;

import com.google.gson.*;
import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.setting.Setting;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and loads client configuration using JSON.
 * No remote telemetry, no data collection — purely local.
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    private final Path configFile;

    public ConfigManager() {
        configDir = Path.of(System.getProperty("user.home"), ".chaosclient");
        configFile = configDir.resolve("config.json");
    }

    public void save() {
        try {
            Files.createDirectories(configDir);

            JsonObject root = new JsonObject();
            root.addProperty("version", ChaosClient.CLIENT_VERSION);

            JsonObject modules = new JsonObject();
            for (Module module : ChaosClient.getInstance().getModuleManager().getModules()) {
                JsonObject moduleObj = new JsonObject();
                moduleObj.addProperty("enabled", module.isEnabled());
                moduleObj.addProperty("keyBind", module.getKeyBind());

                JsonObject settingsObj = new JsonObject();
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting bs) {
                        settingsObj.addProperty(setting.getName(), bs.isEnabled());
                    } else if (setting instanceof NumberSetting ns) {
                        settingsObj.addProperty(setting.getName(), ns.getValue());
                    } else if (setting instanceof ModeSetting ms) {
                        settingsObj.addProperty(setting.getName(), ms.getMode());
                    }
                }
                moduleObj.add("settings", settingsObj);
                modules.add(module.getName(), moduleObj);
            }

            root.add("modules", modules);
            Files.writeString(configFile, GSON.toJson(root));
            ChaosClient.LOGGER.info("Config saved to {}", configFile);
        } catch (IOException e) {
            ChaosClient.LOGGER.error("Failed to save config", e);
        }
    }

    public void load() {
        if (!Files.exists(configFile)) {
            ChaosClient.LOGGER.info("No config file found, using defaults.");
            return;
        }

        try {
            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("modules")) return;
            JsonObject modules = root.getAsJsonObject("modules");

            for (Module module : ChaosClient.getInstance().getModuleManager().getModules()) {
                if (!modules.has(module.getName())) continue;
                JsonObject moduleObj = modules.getAsJsonObject(module.getName());

                // Load enabled state
                if (moduleObj.has("enabled") && moduleObj.get("enabled").getAsBoolean()) {
                    if (!module.isEnabled()) module.toggle();
                }

                // Load keybind
                if (moduleObj.has("keyBind")) {
                    module.setKeyBind(moduleObj.get("keyBind").getAsInt());
                }

                // Load settings
                if (moduleObj.has("settings")) {
                    JsonObject settingsObj = moduleObj.getAsJsonObject("settings");
                    for (Setting setting : module.getSettings()) {
                        if (!settingsObj.has(setting.getName())) continue;

                        if (setting instanceof BooleanSetting bs) {
                            bs.setEnabled(settingsObj.get(setting.getName()).getAsBoolean());
                        } else if (setting instanceof NumberSetting ns) {
                            ns.setValue(settingsObj.get(setting.getName()).getAsDouble());
                        } else if (setting instanceof ModeSetting ms) {
                            ms.setMode(settingsObj.get(setting.getName()).getAsString());
                        }
                    }
                }
            }

            ChaosClient.LOGGER.info("Config loaded from {}", configFile);
        } catch (Exception e) {
            ChaosClient.LOGGER.error("Failed to load config", e);
        }
    }

    public Path getConfigDir() {
        return configDir;
    }

    public boolean hasConfig() {
        return Files.exists(configFile);
    }
}
