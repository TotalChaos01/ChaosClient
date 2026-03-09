package me.totalchaos01.chaosclient.bootstrap;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * PreLaunch entrypoint — runs BEFORE Minecraft classes are loaded.
 * 
 * This is the earliest possible initialization point in Fabric.
 * At this stage:
 * - Minecraft classes are NOT yet loaded
 * - Mixins are registered but NOT yet applied
 * - We can set up class transformers, register hooks, etc.
 * 
 * ChaosClient uses this to:
 * 1. Verify the environment
 * 2. Set system properties for anti-detection
 * 3. Initialize the agent if available
 * 4. Prepare for full initialization (which happens in ChaosClient.onInitializeClient)
 */
public class ChaosPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        System.out.println("[ChaosClient] PreLaunch initialization...");

        // Check if we're running as a Java Agent (deepest integration)
        if (ChaosAgent.isAgentLoaded()) {
            System.out.println("[ChaosClient] Running with Agent instrumentation — full access enabled.");
        }

        // Anti-detection: Set system properties to hide from basic checks
        System.setProperty("chaosclient.loaded", "true");
        System.setProperty("chaosclient.version", "1.3.0");

        // Remove traces from mod detection
        AntiDetection.init();

        System.out.println("[ChaosClient] PreLaunch complete.");
    }
}
