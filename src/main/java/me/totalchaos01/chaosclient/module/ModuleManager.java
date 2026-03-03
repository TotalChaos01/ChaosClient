package me.totalchaos01.chaosclient.module;

import me.totalchaos01.chaosclient.module.impl.combat.*;
import me.totalchaos01.chaosclient.module.impl.movement.*;
import me.totalchaos01.chaosclient.module.impl.player.*;
import me.totalchaos01.chaosclient.module.impl.render.*;
import me.totalchaos01.chaosclient.module.impl.ghost.*;
import me.totalchaos01.chaosclient.module.impl.exploits.*;
import me.totalchaos01.chaosclient.module.impl.other.*;
import me.totalchaos01.chaosclient.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all modules in ChaosClient.
 */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // Combat
        register(new KillAura());
        register(new Velocity());
        register(new Criticals());
        register(new AutoTotem());
        register(new Backtrack());
        register(new ShieldBreak());
        register(new AutoShield());
        register(new AutoBlocking());
        register(new AutoGapple());

        // Movement
        register(new Sprint());
        register(new Fly());
        register(new Speed());
        register(new NoSlow());
        register(new Step());
        register(new Jesus());
        register(new SafeWalk());
        register(new NoFall());

        // Player
        register(new AutoTool());
        register(new FastPlace());
        register(new Scaffold());
        register(new ChestStealer());
        register(new Blink());
        register(new Zoom());
        register(new NoDelay());

        // Legit
        register(new AimAssist());
        register(new AutoClicker());
        register(new Reach());
        register(new Hitbox());

        // Render
        register(new HUD());
        register(new ClickGui());
        register(new ESP());
        register(new Fullbright());
        register(new Tracers());
        register(new Nametags());
        register(new ChestESP());
        register(new NoWeather());
        register(new TargetHUD());
        register(new HUDEditor());

        // Exploits
        register(new ServerCrasher());
        register(new Timer());
        register(new ClientSpoofer());
        register(new Disabler());
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) modules.stream()
                .filter(m -> m.getClass() == clazz)
                .findFirst()
                .orElse(null);
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .toList();
    }

    public Setting getSetting(String moduleName, String settingName) {
        Module module = getModule(moduleName);
        if (module == null) return null;
        return module.getSettings().stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName))
                .findFirst()
                .orElse(null);
    }
}

