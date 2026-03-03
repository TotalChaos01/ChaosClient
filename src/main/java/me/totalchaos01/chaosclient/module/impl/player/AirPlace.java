package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * AirPlace — allows placing blocks in mid-air without needing an adjacent surface.
 */
@ModuleInfo(name = "AirPlace", description = "Place blocks in mid-air", category = Category.PLAYER)
public class AirPlace extends Module {

    private final NumberSetting range = new NumberSetting("Range", 4.5, 1.0, 6.0, 0.5);

    public AirPlace() {
        addSettings(range);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.useKey.isPressed()) return;

        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem)) return;

        // Get the block position the player is looking at
        var hitResult = mc.crosshairTarget;
        if (hitResult == null) return;

        if (hitResult instanceof BlockHitResult blockHit) {
            // Normal placement - handled by vanilla
            return;
        }

        // Air placement: use ray-traced position
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = eyePos.add(lookVec.multiply(range.getValue()));

        BlockPos airPos = new BlockPos(
            (int) Math.floor(targetPos.x),
            (int) Math.floor(targetPos.y),
            (int) Math.floor(targetPos.z)
        );

        if (!mc.world.getBlockState(airPos).isAir()) return;

        BlockHitResult airHit = new BlockHitResult(
            Vec3d.ofCenter(airPos), Direction.UP, airPos, false
        );
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, airHit);
    }
}
