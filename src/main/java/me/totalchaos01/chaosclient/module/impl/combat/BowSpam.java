package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * BowSpam — rapidly spams bow shots by instantly pulling and releasing.
 */
@ModuleInfo(name = "BowSpam", description = "Rapid bow spam", category = Category.COMBAT)
public class BowSpam extends Module {

    private final NumberSetting chargeTime = new NumberSetting("Charge Ticks", 3, 1, 20, 1);
    private final BooleanSetting onlyBow = new BooleanSetting("Only Bow", true);

    private int ticks = 0;

    public BowSpam() {
        addSettings(chargeTime, onlyBow);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        var mainHand = mc.player.getMainHandStack();
        var offHand = mc.player.getOffHandStack();

        boolean holdingBow = mainHand.getItem() == Items.BOW || offHand.getItem() == Items.BOW;
        boolean holdingCrossbow = mainHand.getItem() == Items.CROSSBOW || offHand.getItem() == Items.CROSSBOW;

        if (onlyBow.isEnabled() && !holdingBow) return;
        if (!holdingBow && !holdingCrossbow) return;

        if (mc.player.isUsingItem()) {
            ticks++;
            if (ticks >= chargeTime.getValue()) {
                mc.interactionManager.stopUsingItem(mc.player);
                ticks = 0;
            }
        } else {
            ticks = 0;
        }
    }

    @Override
    public String getSuffix() {
        return String.valueOf((int) chargeTime.getValue());
    }
}
