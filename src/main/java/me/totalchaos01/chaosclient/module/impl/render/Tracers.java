package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender3D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Draws lines from the player to other entities.
 */
@ModuleInfo(name = "Tracers", description = "Draws tracer lines to entities", category = Category.RENDER)
public class Tracers extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 256, 1);

    public Tracers() {
        addSettings(players, mobs, range);
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        // Tracer rendering would use RenderSystem and BufferBuilder
        // to draw lines from screen center to entity positions.
        // Full GL implementation to be added with proper render utils.
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (mc.player.distanceTo(entity) > range.getValue()) continue;
            if (entity instanceof PlayerEntity && !players.isEnabled()) continue;
        }
    }
}
