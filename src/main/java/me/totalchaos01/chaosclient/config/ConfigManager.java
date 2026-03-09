package me.totalchaos01.chaosclient.config;

import com.google.gson.*;
import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.command.impl.FriendCommand;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.setting.Setting;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.ui.clickgui.ClickGuiScreen;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration manager — saves ALL client data to ~/.chaosclient/
 *
 * Directory structure:
 *   ~/.chaosclient/
 *     config.json        — modules, settings, keybinds
 *     friends.json       — friend list
 *     gui.json           — GUI theme, ClickGUI settings
 *     hud.json           — HUD element positions
 *     logs/              — client logs (future)
 *     mods/              — mod jars (deployMod target)
 *     libraries/         — classpath jars (deployLibrary target)
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configDir;
    private final Path configFile;
    private final Path friendsFile;
    private final Path guiFile;
    private final Path hudFile;

    public ConfigManager() {
        configDir = Path.of(System.getProperty("user.home"), ".chaosclient");
        configFile = configDir.resolve("config.json");
        friendsFile = configDir.resolve("friends.json");
        guiFile = configDir.resolve("gui.json");
        hudFile = configDir.resolve("hud.json");
    }

    public void save() {
        try {
            Files.createDirectories(configDir);
            saveModules();
            saveFriends();
            saveGui();
            saveHud();
            ChaosClient.LOGGER.info("Config saved to {}", configDir);
        } catch (IOException e) {
            ChaosClient.LOGGER.error("Failed to save config", e);
        }
    }

    public void load() {
        loadModules();
        loadFriends();
        loadGui();
        loadHud();
        ChaosClient.LOGGER.info("Config loaded from {}", configDir);
    }

    // ─── Modules ──────────────────────────────────────────

    private void saveModules() throws IOException {
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
    }

    private void loadModules() {
        if (!Files.exists(configFile)) return;
        try {
            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("modules")) return;
            JsonObject modules = root.getAsJsonObject("modules");

            for (Module module : ChaosClient.getInstance().getModuleManager().getModules()) {
                if (!modules.has(module.getName())) continue;
                JsonObject moduleObj = modules.getAsJsonObject(module.getName());

                if (moduleObj.has("enabled") && moduleObj.get("enabled").getAsBoolean()) {
                    if (!module.isEnabled()) module.toggle();
                }
                if (moduleObj.has("keyBind")) {
                    module.setKeyBind(moduleObj.get("keyBind").getAsInt());
                }
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
        } catch (Exception e) {
            ChaosClient.LOGGER.error("Failed to load modules config", e);
        }
    }

    // ─── Friends ──────────────────────────────────────────

    private void saveFriends() throws IOException {
        JsonArray arr = new JsonArray();
        for (String friend : FriendCommand.friends) {
            arr.add(friend);
        }
        Files.writeString(friendsFile, GSON.toJson(arr));
    }

    private void loadFriends() {
        if (!Files.exists(friendsFile)) return;
        try {
            String json = Files.readString(friendsFile);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            FriendCommand.friends.clear();
            for (JsonElement el : arr) {
                FriendCommand.friends.add(el.getAsString());
            }
        } catch (Exception e) {
            ChaosClient.LOGGER.error("Failed to load friends", e);
        }
    }

    // ─── GUI ──────────────────────────────────────────────

    private void saveGui() throws IOException {
        JsonObject gui = new JsonObject();
        gui.addProperty("theme", ThemeUtil.getTheme());
        gui.addProperty("gradientSpeed", ClickGuiScreen.getGradientSpeed());
        gui.addProperty("glowIntensity", ClickGuiScreen.getGlowIntensity());
        gui.addProperty("darkMode", ClickGuiScreen.isDarkMode());
        gui.addProperty("shadowEnabled", ClickGuiScreen.isShadowEnabled());
        gui.addProperty("glowEnabled", ClickGuiScreen.isGlowEnabled());
        gui.addProperty("nameProtect", ClickGuiScreen.isNameProtect());
        gui.addProperty("baseColor", ThemeUtil.getBaseColor().getRGB());
        Files.writeString(guiFile, GSON.toJson(gui));
    }

    private void loadGui() {
        if (!Files.exists(guiFile)) {
            // Migration: try loading from old config.json "gui" section
            migrateGuiFromLegacy();
            return;
        }
        try {
            String json = Files.readString(guiFile);
            JsonObject gui = JsonParser.parseString(json).getAsJsonObject();
            if (gui.has("theme")) ThemeUtil.setTheme(gui.get("theme").getAsString());
            if (gui.has("gradientSpeed")) ClickGuiScreen.setGradientSpeed(gui.get("gradientSpeed").getAsFloat());
            if (gui.has("glowIntensity")) ClickGuiScreen.setGlowIntensity(gui.get("glowIntensity").getAsFloat());
            if (gui.has("darkMode")) ClickGuiScreen.setDarkMode(gui.get("darkMode").getAsBoolean());
            if (gui.has("shadowEnabled")) ClickGuiScreen.setShadowEnabled(gui.get("shadowEnabled").getAsBoolean());
            if (gui.has("glowEnabled")) ClickGuiScreen.setGlowEnabled(gui.get("glowEnabled").getAsBoolean());
            if (gui.has("nameProtect")) ClickGuiScreen.setNameProtect(gui.get("nameProtect").getAsBoolean());
            if (gui.has("baseColor")) ThemeUtil.setBaseColor(new Color(gui.get("baseColor").getAsInt(), true));
        } catch (Exception e) {
            ChaosClient.LOGGER.error("Failed to load GUI config", e);
        }
    }

    // ─── HUD ──────────────────────────────────────────────

    private void saveHud() throws IOException {
        try {
            if (ChaosClient.getInstance().getHudManager() != null) {
                Files.writeString(hudFile, GSON.toJson(ChaosClient.getInstance().getHudManager().toJson()));
            }
        } catch (Exception ignored) {}
    }

    private void loadHud() {
        if (!Files.exists(hudFile)) {
            migrateHudFromLegacy();
            return;
        }
        try {
            if (ChaosClient.getInstance().getHudManager() != null) {
                String json = Files.readString(hudFile);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                ChaosClient.getInstance().getHudManager().init();
                ChaosClient.getInstance().getHudManager().fromJson(obj);
            }
        } catch (Exception ignored) {}
    }

    // ─── Legacy migration (from single config.json with gui/hud sections) ────

    private void migrateGuiFromLegacy() {
        if (!Files.exists(configFile)) return;
        try {
            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("gui")) {
                JsonObject gui = root.getAsJsonObject("gui");
                if (gui.has("theme")) ThemeUtil.setTheme(gui.get("theme").getAsString());
                if (gui.has("gradientSpeed")) ClickGuiScreen.setGradientSpeed(gui.get("gradientSpeed").getAsFloat());
                if (gui.has("glowIntensity")) ClickGuiScreen.setGlowIntensity(gui.get("glowIntensity").getAsFloat());
                if (gui.has("darkMode")) ClickGuiScreen.setDarkMode(gui.get("darkMode").getAsBoolean());
                if (gui.has("shadowEnabled")) ClickGuiScreen.setShadowEnabled(gui.get("shadowEnabled").getAsBoolean());
                if (gui.has("glowEnabled")) ClickGuiScreen.setGlowEnabled(gui.get("glowEnabled").getAsBoolean());
                if (gui.has("nameProtect")) ClickGuiScreen.setNameProtect(gui.get("nameProtect").getAsBoolean());
                if (gui.has("baseColor")) ThemeUtil.setBaseColor(new Color(gui.get("baseColor").getAsInt(), true));
                ChaosClient.LOGGER.info("Migrated GUI config from legacy config.json");
            }
        } catch (Exception ignored) {}
    }

    private void migrateHudFromLegacy() {
        if (!Files.exists(configFile)) return;
        try {
            String json = Files.readString(configFile);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("hud") && ChaosClient.getInstance().getHudManager() != null) {
                ChaosClient.getInstance().getHudManager().init();
                ChaosClient.getInstance().getHudManager().fromJson(root.getAsJsonObject("hud"));
                ChaosClient.LOGGER.info("Migrated HUD config from legacy config.json");
            }
        } catch (Exception ignored) {}
    }

    // ─── Accessors ────────────────────────────────────────

    public Path getConfigDir() { return configDir; }
    public boolean hasConfig() { return Files.exists(configFile); }
}
