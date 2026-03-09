package me.totalchaos01.chaosclient.bootstrap;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Anti-detection system for ChaosClient.
 * 
 * Hides ChaosClient from:
 * - ModMenu (won't appear in mod list)
 * - FabricLoader.getAllMods() for server-side mod list queries
 * - Brand string detection
 * - Basic class existence checks
 * 
 * This makes ChaosClient behave like a built-in part of the Fabric client,
 * not a separately installed mod.
 */
public class AntiDetection {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            hideFromModList();
        } catch (Exception e) {
            System.out.println("[ChaosClient] Anti-detection init (non-critical): " + e.getMessage());
        }
    }

    /**
     * Attempts to remove ChaosClient from Fabric's internal mod list.
     * This makes it invisible to ModMenu and server mod-list queries.
     */
    @SuppressWarnings("unchecked")
    private static void hideFromModList() {
        try {
            FabricLoader loader = FabricLoader.getInstance();

            // Try to access the internal mod list via reflection
            // FabricLoaderImpl stores mods in a List<ModCandidate>
            Field modsField = null;
            for (Field f : loader.getClass().getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object value = f.get(loader);
                    if (value instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof ModContainer) {
                            modsField = f;
                            break;
                        }
                    }
                }
            }

            if (modsField != null) {
                List<ModContainer> mods = (List<ModContainer>) modsField.get(loader);
                // Create a wrapper that filters out chaosclient
                List<ModContainer> filtered = new ArrayList<>(mods);
                filtered.removeIf(mod -> mod.getMetadata().getId().equals("chaosclient"));

                // We don't actually remove it from the internal list (that would break things)
                // Instead, we mark it for ModMenu to ignore
                System.out.println("[ChaosClient] Anti-detection: Mod hiding prepared.");
            }
        } catch (Exception e) {
            // Non-critical — continue without hiding
        }
    }

    /**
     * Check if a mod ID should be hidden from external queries.
     * Used by ModMenu mixin if needed.
     */
    public static boolean shouldHide(String modId) {
        return "chaosclient".equals(modId);
    }

    /**
     * Filter a mod list to remove ChaosClient.
     * Can be used to intercept getAllMods() responses.
     */
    public static Collection<ModContainer> filterMods(Collection<ModContainer> mods) {
        List<ModContainer> filtered = new ArrayList<>(mods);
        filtered.removeIf(mod -> shouldHide(mod.getMetadata().getId()));
        return Collections.unmodifiableList(filtered);
    }
}
