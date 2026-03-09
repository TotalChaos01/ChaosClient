package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

@ModuleInfo(name = "Hitbox", description = "Expands enemy hitboxes", category = Category.LEGIT)
public class Hitbox extends Module {

    private static Hitbox INSTANCE;

    private final NumberSetting size = new NumberSetting("Size", 0.12, 0.0, 0.8, 0.01);

    public Hitbox() {
        INSTANCE = this;
        addSettings(size);
    }

    public static Hitbox get() {
        return INSTANCE;
    }

    public float getSize() {
        return (float) size.getValue();
    }
}

