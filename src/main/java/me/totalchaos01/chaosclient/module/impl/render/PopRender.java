package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketReceive;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * PopRender — renders visual effects when a player pops a totem of undying.
 */
@ModuleInfo(name = "PopRender", description = "Visual effect on totem pops", category = Category.RENDER)
public class PopRender extends Module {

    private final NumberSetting duration = new NumberSetting("Duration (s)", 2.0, 0.5, 5.0, 0.5);
    private final BooleanSetting ownPops = new BooleanSetting("Own Pops", false);

    private final List<PopEffect> effects = new ArrayList<>();

    public PopRender() {
        addSettings(duration, ownPops);
    }

    @EventTarget
    public void onPacketReceive(EventPacketReceive event) {
        if (mc.world == null || mc.player == null) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 35) { // Totem pop
                var entity = packet.getEntity(mc.world);
                if (entity instanceof PlayerEntity player) {
                    if (player == mc.player && !ownPops.isEnabled()) return;

                    effects.add(new PopEffect(
                        new Vec3d(player.getX(), player.getY(), player.getZ()),
                        System.currentTimeMillis(),
                        player.getName().getString()
                    ));
                }
            }
        }
    }

    @EventTarget
    public void onTick(EventTick event) {
        // Remove expired effects
        long now = System.currentTimeMillis();
        long maxAge = (long) (duration.getValue() * 1000);
        effects.removeIf(e -> now - e.time > maxAge);
    }

    public List<PopEffect> getEffects() {
        return effects;
    }

    @Override
    protected void onDisable() {
        effects.clear();
    }

    public record PopEffect(Vec3d position, long time, String playerName) {}
}
