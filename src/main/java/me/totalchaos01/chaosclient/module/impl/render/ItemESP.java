package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender3D;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * ItemESP module — ported from LiquidBounce
 *
 * Highlights dropped items, arrows, and tridents through walls using glow effect.
 * Uses the existing entity glow system from MixinEntity.
 */
@ModuleInfo(name = "ItemESP", description = "Highlights dropped items through walls", category = Category.RENDER)
public class ItemESP extends Module {

    private final NumberSetting maxDistance = new NumberSetting("Max Distance", 128, 16, 512, 8);
    private final BooleanSetting showArrows = new BooleanSetting("Show Arrows", true);
    private final BooleanSetting showTridents = new BooleanSetting("Show Tridents", true);

    private final List<Entity> trackedItems = new ArrayList<>();

    public ItemESP() {
        addSettings(maxDistance, showArrows, showTridents);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.world == null || mc.player == null) return;

        trackedItems.clear();
        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null) continue;
            if (mc.player.squaredDistanceTo(entity) > maxDistSq) continue;

            if (entity instanceof ItemEntity) {
                trackedItems.add(entity);
            } else if (showArrows.isEnabled() && entity instanceof net.minecraft.entity.projectile.ArrowEntity) {
                trackedItems.add(entity);
            } else if (showArrows.isEnabled() && entity instanceof net.minecraft.entity.projectile.SpectralArrowEntity) {
                trackedItems.add(entity);
            } else if (showTridents.isEnabled() && entity instanceof net.minecraft.entity.projectile.TridentEntity) {
                trackedItems.add(entity);
            }
        }
    }

    /**
     * Used by MixinEntity to check if this entity should glow for ItemESP.
     */
    public boolean shouldGlow(Entity entity) {
        return isEnabled() && trackedItems.contains(entity);
    }

    @Override
    protected void onDisable() {
        trackedItems.clear();
    }
}
