package me.totalchaos01.chaosclient.module;

import me.totalchaos01.chaosclient.module.impl.combat.KillAura;
import me.totalchaos01.chaosclient.module.impl.render.*;
import me.totalchaos01.chaosclient.module.impl.exploits.*;
import me.totalchaos01.chaosclient.module.impl.ghost.AimAssist;
import me.totalchaos01.chaosclient.module.impl.ghost.Hitbox;
import me.totalchaos01.chaosclient.module.impl.ghost.Reach;
import me.totalchaos01.chaosclient.module.impl.ghost.TriggerBot;
import me.totalchaos01.chaosclient.module.impl.other.BaritoneModule;
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

        // Render
        register(new HUD());
        register(new ClickGui());
        register(new ESP());
        register(new Fullbright());
        register(new Tracers());
        register(new ClientViewDebug());
        register(new Nametags());
        register(new ChestESP());
        register(new NoWeather());
        register(new TargetHUD());
        register(new Freecam());
        register(new HoleESP());
        register(new StorageESP());
        register(new Ambience());
        register(new HandView());
        register(new LogoutSpots());
        register(new NewChunks());
        register(new XRAY());
        register(new PopRender());
        register(new AntiBlind());
        register(new NoBob());
        register(new ItemESP());
        register(new TrueSight());
        register(new NoSwing());
        register(new Breadcrumbs());
        register(new HitParticles());

        // Legit
        register(new AimAssist());
        register(new TriggerBot());
        register(new Reach());
        register(new Hitbox());

        // Exploits
        register(new ServerCrasher());
        register(new Damage());
        register(new Kick());
        register(new VehicleOneHit());
        register(new MoreCarry());
        register(new PingSpoof());
        register(new Clip());
        register(new Disabler());
        register(new Plugins());
        register(new AntiHunger());

        // Other
        register(new BaritoneModule());
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
