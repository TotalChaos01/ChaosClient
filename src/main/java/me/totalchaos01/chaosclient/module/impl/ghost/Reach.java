package me.totalchaos01.chaosclient.module.impl.ghost;

import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;

@ModuleInfo(name = "Reach", description = "Increases attack reach in legit style", category = Category.LEGIT)
public class Reach extends Module {

    private static Reach INSTANCE;

    private final NumberSetting value = new NumberSetting("Value", 0.35, 0.0, 3.0, 0.05);

    public Reach() {
        INSTANCE = this;
        addSettings(value);
    }

    public static Reach get() {
        return INSTANCE;
    }

    public float getValue() {
        return (float) value.getValue();
    }
}

