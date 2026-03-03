package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.ColorUtil;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import me.totalchaos01.chaosclient.util.render.ThemeType;
import me.totalchaos01.chaosclient.util.render.ThemeUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

/**
 * Redesigned Nametags with Rise-inspired visuals:
 * skin face, themed health bar, rounded background, armor display.
 */
@ModuleInfo(name = "Nametags", description = "Enhanced player nametags", category = Category.RENDER)
public class Nametags extends Module {

    private final BooleanSetting health = new BooleanSetting("Health", true);
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting armor = new BooleanSetting("Armor", true);
    private final BooleanSetting ping = new BooleanSetting("Ping", true);
    private final BooleanSetting distance = new BooleanSetting("Distance", true);
    private final BooleanSetting skinFace = new BooleanSetting("Skin Face", true);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.5, 3.0, 0.1);
    private final NumberSetting range = new NumberSetting("Range", 64, 8, 128, 1);

    public Nametags() {
        addSettings(health, healthBar, armor, ping, distance, skinFace, background, scale, range);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || mc.world == null) return;
        DrawContext ctx = event.getDrawContext();
        float tickDelta = event.getTickDelta();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > range.getValue()) continue;

            renderNametag(ctx, player, tickDelta);
        }
    }

    private void renderNametag(DrawContext ctx, PlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);
        double headY = pos.y + player.getHeight() + 0.3;

        double[] screen = RenderUtil.worldToScreen(pos.x, headY, pos.z);
        if (screen == null) return;

        double dist = mc.player.distanceTo(player);
        double dynamicScale = scale.getValue() * Math.max(0.4, Math.min(2.0, 6.0 / Math.max(dist, 1.0)));

        // Push matrix for scaling
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) screen[0], (float) screen[1]);
        ctx.getMatrices().scale((float) dynamicScale, (float) dynamicScale);
        ctx.getMatrices().translate((float) -screen[0], (float) -screen[1]);

        // Build nametag content
        String name = player.getName().getString();
        float hp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float hpRatio = Math.max(0, Math.min(1, hp / maxHp));

        // Calculate text parts
        StringBuilder info = new StringBuilder(name);
        if (health.isEnabled()) {
            info.append(" ").append(getHealthColorCode(hpRatio)).append(String.format("%.1f", hp)).append("\u2764");
        }
        if (ping.isEnabled()) {
            PlayerListEntry entry = mc.getNetworkHandler() != null
                    ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) : null;
            int pingVal = entry != null ? entry.getLatency() : -1;
            if (pingVal >= 0) {
                info.append(" \u00A77").append(pingVal).append("ms");
            }
        }
        if (distance.isEnabled()) {
            info.append(" \u00A78[\u00A77").append(String.format("%.0fm", dist)).append("\u00A78]");
        }

        String infoStr = info.toString();
        int textWidth = mc.textRenderer.getWidth(infoStr);
        int faceSize = skinFace.isEnabled() ? 10 : 0;
        int totalWidth = textWidth + faceSize + (faceSize > 0 ? 3 : 0) + 8;
        int totalHeight = healthBar.isEnabled() ? 19 : 14;

        int bgX = (int) screen[0] - totalWidth / 2;
        int bgY = (int) screen[1] - totalHeight - 2;

        // Background
        if (background.isEnabled()) {
            RenderUtil.roundedRectSimple(ctx, bgX - 1, bgY - 1, totalWidth + 2, totalHeight + 2, 5, 0xCC0D0D14);
            RenderUtil.roundedRectOutline(ctx, bgX - 1, bgY - 1, totalWidth + 2, totalHeight + 2, 5, 1, 0x30FFFFFF);
        }

        // Skin face
        int contentX = bgX + 4;
        int contentY = bgY + 2;
        if (skinFace.isEnabled()) {
            RenderUtil.drawPlayerFace(ctx, player, contentX, contentY, faceSize);
            contentX += faceSize + 3;
        }

        // Name and info text
        ctx.drawTextWithShadow(mc.textRenderer, infoStr, contentX, contentY + 1, 0xFFFFFFFF);

        // Health bar
        if (healthBar.isEnabled()) {
            int barY = bgY + totalHeight - 4;
            int barWidth = totalWidth - 4;
            int barX = bgX + 2;

            // Bar background
            RenderUtil.roundedRectSimple(ctx, barX, barY, barWidth, 3, 1, 0xFF1A1A1A);

            // Bar fill with theme color
            int fillWidth = (int) (barWidth * hpRatio);
            if (fillWidth > 0) {
                Color themeColor = ThemeUtil.getThemeColor(0, ThemeType.ARRAYLIST, 1);
                int themeARGB = ColorUtil.toARGB(themeColor);
                int healthColor = hpRatio > 0.5f ? themeARGB : getHealthColorARGB(hpRatio);
                RenderUtil.roundedRectSimple(ctx, barX, barY, fillWidth, 3, 1, healthColor);
            }
        }

        // Armor display above nametag
        if (armor.isEnabled()) {
            int armorX = (int) screen[0] - 32;
            int armorY = bgY - 18;
            // Iterate armor slots: HEAD, CHEST, LEGS, FEET
            net.minecraft.entity.EquipmentSlot[] armorSlots = {
                    net.minecraft.entity.EquipmentSlot.HEAD,
                    net.minecraft.entity.EquipmentSlot.CHEST,
                    net.minecraft.entity.EquipmentSlot.LEGS,
                    net.minecraft.entity.EquipmentSlot.FEET
            };
            for (net.minecraft.entity.EquipmentSlot slot : armorSlots) {
                ItemStack stack = player.getEquippedStack(slot);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, armorX, armorY);
                    armorX += 16;
                }
            }
            ItemStack mainHand = player.getMainHandStack();
            if (!mainHand.isEmpty()) {
                ctx.drawItem(mainHand, armorX + 2, armorY);
            }
        }

        ctx.getMatrices().popMatrix();
    }

    private String getHealthColorCode(float ratio) {
        if (ratio > 0.75f) return "\u00A7a";
        if (ratio > 0.5f) return "\u00A7e";
        if (ratio > 0.25f) return "\u00A76";
        return "\u00A7c";
    }

    private int getHealthColorARGB(float ratio) {
        int r = (int) (255 * (1.0f - ratio));
        int g = (int) (255 * ratio);
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
