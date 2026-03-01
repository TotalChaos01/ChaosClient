package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.ui.clickgui.ClickGuiScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Opens the click GUI for module configuration.
 */
@ModuleInfo(name = "ClickGui", description = "Opens the module configuration GUI", category = Category.RENDER, keyBind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGui extends Module {

    private final ClickGuiScreen screen = new ClickGuiScreen();

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            mc.setScreen(screen);
        }
        toggle(); // immediately disable — it's a one-shot toggle that opens the screen
    }
}
