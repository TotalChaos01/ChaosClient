package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.ui.hud.HUDEditorScreen;

/**
 * Opens the HUD Editor for drag-and-drop repositioning of HUD elements.
 * One-shot toggle — opens the editor screen then disables itself.
 */
@ModuleInfo(name = "HUDEditor", description = "Drag-and-drop HUD element positioning", category = Category.RENDER)
public class HUDEditor extends Module {

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            mc.setScreen(new HUDEditorScreen());
        }
        // Immediately disable — it's a one-shot toggle
        setEnabled(false);
    }
}
