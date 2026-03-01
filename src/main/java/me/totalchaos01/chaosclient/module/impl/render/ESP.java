package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender3D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Renders ESP (Extra Sensory Perception) boxes/outlines around entities.
 */
@ModuleInfo(name = "ESP", description = "Highlights entities through walls", category = Category.RENDER)
public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Box", "Box", "Outline", "Glow");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);

    public ESP() {
        addSettings(mode, players, mobs, range);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (mc.player.distanceTo(entity) > range.getValue()) continue;

            if (entity instanceof PlayerEntity && players.isEnabled()) {
                // Entity rendering will be implemented with proper GL calls
                // For now uses the glow effect as fallback
                if (mode.is("Glow")) {
                    entity.setGlowing(true);
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        // Remove glow from all entities
        if (mc.world != null) {
            for (Entity entity : mc.world.getEntities()) {
                entity.setGlowing(false);
            }
        }
    }
}
