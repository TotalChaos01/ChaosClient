package me.totalchaos01.chaosclient.module.impl.combat;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * AutoCrystal — automatic end crystal placement and breaking for crystal PvP.
 */
@ModuleInfo(name = "AutoCrystal", description = "Automatically places and breaks crystals", category = Category.COMBAT)
public class AutoCrystal extends Module {

    // General
    private final NumberSetting targetRange = new NumberSetting("Target Range", 12, 4, 16, 1);
    private final NumberSetting placeRange = new NumberSetting("Place Range", 5.0, 1.0, 6.0, 0.5);
    private final NumberSetting breakRange = new NumberSetting("Break Range", 5.0, 1.0, 6.0, 0.5);

    // Damage
    private final NumberSetting minDamage = new NumberSetting("Min Damage", 4, 0, 20, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("Max Self Damage", 8, 0, 20, 0.5);

    // Timing
    private final NumberSetting placeDelay = new NumberSetting("Place Delay", 2, 0, 10, 1);
    private final NumberSetting breakDelay = new NumberSetting("Break Delay", 1, 0, 10, 1);

    // Options
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    private final BooleanSetting antiSuicide = new BooleanSetting("Anti Suicide", true);
    private final ModeSetting rotateMode = new ModeSetting("Rotate", "None", "None", "Place", "Break", "Both");

    private int placeTimer = 0;
    private int breakTimer = 0;
    private PlayerEntity target;

    public AutoCrystal() {
        addSettings(targetRange, placeRange, breakRange, minDamage, maxSelfDamage,
                   placeDelay, breakDelay, autoSwitch, antiSuicide, rotateMode);
    }

    @EventTarget
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        placeTimer++;
        breakTimer++;

        // Find target
        target = findTarget();
        if (target == null) return;

        // Break crystals
        if (breakTimer >= breakDelay.getValue()) {
            breakCrystals();
        }

        // Place crystals
        if (placeTimer >= placeDelay.getValue()) {
            placeCrystals();
        }
    }

    private PlayerEntity findTarget() {
        return mc.world.getPlayers().stream()
            .filter(p -> p != mc.player)
            .filter(p -> !p.isDead())
            .filter(p -> mc.player.distanceTo(p) <= targetRange.getValue())
            .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .orElse(null);
    }

    private void breakCrystals() {
        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (mc.player.distanceTo(crystal) > breakRange.getValue()) continue;

            // Anti-suicide check
            if (antiSuicide.isEnabled()) {
                Vec3d crystalVec = new Vec3d(crystal.getX(), crystal.getY(), crystal.getZ());
                float selfDamage = estimateDamage(crystalVec, mc.player);
                if (selfDamage >= mc.player.getHealth()) continue;
                if (selfDamage > maxSelfDamage.getValue()) continue;
            }

            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            breakTimer = 0;
            return; // One break per tick
        }
    }

    private void placeCrystals() {
        // Find crystal slot
        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1) return;

        BlockPos bestPos = null;
        double bestDamage = minDamage.getValue();

        // Search for placement positions
        int r = (int) Math.ceil(placeRange.getValue());
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -3; y <= 1; y++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (!canPlaceCrystal(pos)) continue;
                    if (mc.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > placeRange.getValue() * placeRange.getValue()) continue;

                    Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    float damage = estimateDamage(crystalPos, target);
                    float selfDamage = estimateDamage(crystalPos, mc.player);

                    if (antiSuicide.isEnabled() && selfDamage >= mc.player.getHealth()) continue;
                    if (selfDamage > maxSelfDamage.getValue()) continue;

                    if (damage > bestDamage) {
                        bestDamage = damage;
                        bestPos = pos;
                    }
                }
            }
        }

        if (bestPos != null) {
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            if (autoSwitch.isEnabled()) {
                mc.player.getInventory().setSelectedSlot(crystalSlot);
            }

            BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(bestPos), Direction.UP, bestPos, false
            );
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            placeTimer = 0;

            if (autoSwitch.isEnabled()) {
                mc.player.getInventory().setSelectedSlot(prevSlot);
            }
        }
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        var state = mc.world.getBlockState(pos);
        if (state.getBlock() != Blocks.OBSIDIAN && state.getBlock() != Blocks.BEDROCK) return false;

        BlockPos above = pos.up();
        if (!mc.world.getBlockState(above).isAir()) return false;
        if (!mc.world.getBlockState(above.up()).isAir()) return false;

        // Check no entities in the way
        Box box = new Box(above);
        return mc.world.getOtherEntities(null, box).isEmpty();
    }

    private int findCrystalSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Simplified crystal damage estimation.
     */
    private float estimateDamage(Vec3d crystalPos, PlayerEntity target) {
        double distance = target.squaredDistanceTo(crystalPos);
        if (distance > 12 * 12) return 0;

        // Simplified damage formula
        double realDist = Math.sqrt(distance);
        double impact = (1.0 - realDist / 12.0) * 6.0; // 6 = explosion power
        double damage = (impact * impact + impact) / 2.0 * 7.0 + 1.0;

        // Apply armor reduction (simplified)
        float armorReduction = target.getArmor() * 0.04f;
        damage *= (1.0 - Math.min(armorReduction, 0.8));

        return (float) Math.max(0, damage);
    }

    @Override
    protected void onDisable() {
        target = null;
        placeTimer = 0;
        breakTimer = 0;
    }
}
