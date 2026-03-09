package me.totalchaos01.chaosclient.bootstrap;

import java.lang.instrument.Instrumentation;

/**
 * Java Agent entry point for ChaosClient.
 * 
 * When loaded via -javaagent:ChaosClient.jar, this class initializes
 * BEFORE Fabric Loader and Minecraft, giving the deepest possible
 * integration level.
 * 
 * Usage in launcher:
 *   java -javaagent:ChaosClient-1.3.0.jar [other args] net.fabricmc.loader.impl.launch.knot.KnotClient
 * 
 * The agent stores the Instrumentation instance for later use by
 * ChaosClient modules that need class retransformation capabilities.
 */
public class ChaosAgent {

    private static Instrumentation instrumentation;
    private static boolean agentLoaded = false;

    /**
     * Called when loaded as a Java Agent via -javaagent before main().
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
        agentLoaded = true;
        System.out.println("[ChaosClient] Agent loaded (premain). Deep integration enabled.");
    }

    /**
     * Called when attached to a running VM via Attach API.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
        agentLoaded = true;
        System.out.println("[ChaosClient] Agent attached (agentmain). Deep integration enabled.");
    }

    /**
     * @return true if ChaosClient was loaded as a Java Agent
     */
    public static boolean isAgentLoaded() {
        return agentLoaded;
    }

    /**
     * @return the Instrumentation instance, or null if not loaded as agent
     */
    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
