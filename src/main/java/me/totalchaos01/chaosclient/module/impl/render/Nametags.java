package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/**
 * Renders custom nametags with health bars, armor, and ping information.
 * Uses worldToScreen projection for 2D overlay rendering.
 */
@ModuleInfo(name = "Nametags", description = "Enhanced nametags for players", category = Category.RENDER)
public class Nametags extends Module {

    private final BooleanSetting health = new BooleanSetting("Health", true);
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting armor = new BooleanSetting("Armor", true);
    private final BooleanSetting ping = new BooleanSetting("Ping", true);
    private final BooleanSetting distance = new BooleanSetting("Distance", true);
    private final NumberSetting scale = new NumberSetting("Scale", 1.5, 0.5, 3.0, 0.1);
    private final NumberSetting maxRange = new NumberSetting("Range", 64, 8, 256, 1);
    private final BooleanSetting background = new BooleanSetting("Background", true);

    private static final int BG_COLOR = 0xAA000000;
    private static final int HEALTH_GREEN = 0xFF44FF44;
    private static final int HEALTH_YELLOW = 0xFFFFFF44;
    private static final int HEALTH_RED = 0xFFFF4444;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public Nametags() {
        addSettings(health, healthBar, armor, ping, distance, scale, maxRange, background);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.world == null || mc.player == null) return;

        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (player == mc.player) continue;
            float dist = mc.player.distanceTo(player);
            if (dist > maxRange.getValue()) continue;

            // Get interpolated position at head height + offset
            Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
            double ix = lerpedPos.x;
            double iy = lerpedPos.y + entity.getHeight() + 0.3;
            double iz = lerpedPos.z;

            // Project to screen
            double[] screen = RenderUtil.worldToScreen(ix, iy, iz);
            if (screen == null) continue;

            // Scale factor based on distance (closer = bigger, further = smaller)
            float distScale = (float) scale.getValue();
            float scaleFactor = Math.max(0.5f, distScale - dist / 100f);

            // Build display text
            StringBuilder text = new StringBuilder();
            text.append(player.getName().getString());

            if (health.isEnabled()) {
                float hp = player.getHealth();
                text.append(" \u00a7").append(getHealthColorCode(hp, player.getMaxHealth()));
                text.append(String.format("%.1f\u2764", hp));
            }

            if (ping.isEnabled()) {
                int playerPing = getPlayerPing(player);
                if (playerPing >= 0) {
                    text.append(" \u00a77").append(playerPing).append("ms");
                }
            }

            if (distance.isEnabled()) {
                text.append(" \u00a78[").append(String.format("%.1f", dist)).append("m]");
            }

            String displayText = text.toString();
            int textWidth = mc.textRenderer.getWidth(displayText);
            int textHeight = mc.textRenderer.fontHeight;

            // Apply scaling via matrix (Matrix3x2fStack in 1.21.11)
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate((float) screen[0], (float) screen[1]);
            ctx.getMatrices().scale(scaleFactor, scaleFactor);
            ctx.getMatrices().translate(-(float) screen[0], -(float) screen[1]);

            int tagX = (int) (screen[0] - textWidth / 2.0);
            int tagY = (int) screen[1] - textHeight - 4;
            int padding = 3;

            // Background
            if (background.isEnabled()) {
                RenderUtil.roundedRectSimple(ctx,
                    tagX - padding, tagY - padding,
                    textWidth + padding * 2, textHeight + padding * 2 + (healthBar.isEnabled() ? 4 : 0),
                    3, BG_COLOR);
            }

            // Name text
            ctx.drawTextWithShadow(mc.textRenderer, displayText, tagX, tagY, 0xFFFFFFFF);

            // Health bar below name
            if (healthBar.isEnabled() && player.getMaxHealth() > 0) {
                float healthPct = Math.min(1, player.getHealth() / player.getMaxHealth());
                int barY = tagY + textHeight + 1;
                int barWidth = textWidth;
                int barHeight = 2;

                // Bar background
                RenderUtil.rect(ctx, tagX, barY, barWidth, barHeight, 0x80000000);
                // Bar fill
                int hColor = getHealthBarColor(healthPct);
                RenderUtil.rect(ctx, tagX, barY, (int) (barWidth * healthPct), barHeight, hColor);
            }

            // Armor items above nametag
            if (armor.isEnabled()) {
                int armorY = tagY - padding - 18;
                int armorX = (int) (screen[0] - 40);
                int slot = 0;
                for (EquipmentSlot armorSlot : ARMOR_SLOTS) {
                    ItemStack stack = player.getEquippedStack(armorSlot);
                    if (!stack.isEmpty()) {
                        ctx.drawItem(stack, armorX + slot * 16, armorY);
                    }
                    slot++;
                }
                // Main hand item
                ItemStack mainHand = player.getMainHandStack();
                if (!mainHand.isEmpty()) {
                    ctx.drawItem(mainHand, armorX + slot * 16, armorY);
                }
            }

            ctx.getMatrices().popMatrix();
        }
    }

    private char getHealthColorCode(float health, float maxHealth) {
        float pct = health / maxHealth;
        if (pct > 0.75f) return 'a'; // Green
        if (pct > 0.5f) return 'e';  // Yellow
        if (pct > 0.25f) return '6'; // Orange
        return 'c'; // Red
    }

    private int getHealthBarColor(float pct) {
        if (pct > 0.75f) return HEALTH_GREEN;
        if (pct > 0.5f) return HEALTH_YELLOW;
        return HEALTH_RED;
    }

    private int getPlayerPing(PlayerEntity player) {
        if (mc.getNetworkHandler() == null) return -1;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry != null ? entry.getLatency() : -1;
    }
}
