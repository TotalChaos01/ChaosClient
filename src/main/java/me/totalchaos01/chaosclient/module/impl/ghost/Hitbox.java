package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

/**
 * Expands entity hitboxes client-side for easier targeting.
 * The expanded hitbox is only used for raycast/targeting — 
 * actual rendering is unaffected. Handled via MixinEntity.
 */
@ModuleInfo(name = "Hitbox", description = "Expands entity hitboxes", category = Category.LEGIT)
public class Hitbox extends Module {

    private final NumberSetting expand = new NumberSetting("Expand", 0.2, 0.05, 0.5, 0.05);

    public Hitbox() {
        addSettings(expand);
    }

    public double getExpand() {
        return expand.getValue();
    }

    @Override
    public String getSuffix() {
        return String.format("%.2f", expand.getValue());
    }
}
