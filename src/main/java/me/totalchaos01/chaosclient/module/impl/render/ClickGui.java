package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.ui.clickgui.ClickGuiScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the Rise-style Click GUI for module configuration.
 * Press Right Shift to open.
 */
@ModuleInfo(name = "ClickGui", description = "Opens the Rise-style configuration GUI", category = Category.RENDER, keyBind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGui extends Module {

    private ClickGuiScreen screen;

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            if (screen == null) {
                screen = new ClickGuiScreen();
            }
            mc.setScreen(screen);
        }
        // Immediately disable — it's a one-shot toggle
        setEnabled(false);
    }
}
