package me.totalchaos01.chaosclient.module.impl.render;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventPacketSend;
import me.totalchaos01.chaosclient.event.events.EventRender2D;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.ModeSetting;
import me.totalchaos01.chaosclient.setting.impl.NumberSetting;
import me.totalchaos01.chaosclient.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * HitParticles — spawns visual particles at the point of impact when attacking entities.
 * Renders colored particles that fly outward from the hit location and fade over time.
 */
@ModuleInfo(name = "HitParticles", description = "Shows particles when hitting entities", category = Category.RENDER)
public class HitParticles extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Stars", "Stars", "Hearts", "Sparks", "Blood");
    private final NumberSetting amount = new NumberSetting("Amount", 8, 2, 30, 1);
    private final NumberSetting lifetime = new NumberSetting("Lifetime", 20, 5, 40, 1);
    private final NumberSetting size = new NumberSetting("Size", 3.0, 1.0, 8.0, 0.5);

    private final List<HitParticle> particles = new ArrayList<>();

    public HitParticles() {
        addSettings(mode, amount, lifetime, size);
    }

    @EventTarget
    public void onPacketSend(EventPacketSend event) {
        if (mc.player == null || mc.world == null) return;

        // Detect attack packets to spawn particles
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            // Get the targeted entity from mc.targetedEntity
            Entity target = mc.targetedEntity;
            if (target == null) return;

            Vec3d hitPos = target.getBoundingBox().getCenter();
            spawnParticles(hitPos);
        }
    }

    private void spawnParticles(Vec3d position) {
        int count = (int) amount.getValue();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            double vx = (rand.nextDouble() - 0.5) * 0.15;
            double vy = rand.nextDouble() * 0.1 + 0.02;
            double vz = (rand.nextDouble() - 0.5) * 0.15;

            // Slight position randomization
            double px = position.x + (rand.nextDouble() - 0.5) * 0.6;
            double py = position.y + (rand.nextDouble() - 0.5) * 0.6;
            double pz = position.z + (rand.nextDouble() - 0.5) * 0.6;

            int color = getParticleColor(rand);
            int life = (int) lifetime.getValue() + rand.nextInt(-3, 4);

            particles.add(new HitParticle(px, py, pz, vx, vy, vz, color, Math.max(5, life)));
        }
    }

    private int getParticleColor(ThreadLocalRandom rand) {
        return switch (mode.getMode()) {
            case "Stars" -> {
                int[] colors = {0xFFFFFF00, 0xFFFFAA00, 0xFFFF8800, 0xFFFFDD00};
                yield colors[rand.nextInt(colors.length)];
            }
            case "Hearts" -> {
                int[] colors = {0xFFFF3366, 0xFFFF6688, 0xFFFF4477, 0xFFFF2255};
                yield colors[rand.nextInt(colors.length)];
            }
            case "Sparks" -> {
                int[] colors = {0xFFFFFFFF, 0xFFAADDFF, 0xFF88CCFF, 0xFFCCEEFF};
                yield colors[rand.nextInt(colors.length)];
            }
            case "Blood" -> {
                int[] colors = {0xFFBB0000, 0xFF880000, 0xFFAA1111, 0xFFCC2222};
                yield colors[rand.nextInt(colors.length)];
            }
            default -> 0xFFFFFFFF;
        };
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null || particles.isEmpty()) return;

        DrawContext ctx = event.getDrawContext();

        Iterator<HitParticle> iter = particles.iterator();
        while (iter.hasNext()) {
            HitParticle p = iter.next();
            p.tick();

            if (p.isDead()) {
                iter.remove();
                continue;
            }

            double[] screen = RenderUtil.worldToScreen(p.x, p.y, p.z);
            if (screen == null || screen[2] < 0 || screen[2] > 1) continue;

            float sx = (float) screen[0];
            float sy = (float) screen[1];
            float alpha = p.getAlpha();
            int alphaInt = (int) (alpha * 255);
            int col = (p.color & 0x00FFFFFF) | (alphaInt << 24);

            float s = (float) size.getValue() * (1.0f - (1.0f - alpha) * 0.5f);
            int half = Math.max(1, (int) s);

            if (mode.is("Stars")) {
                // Draw star shape (+ cross)
                ctx.fill((int) sx - half, (int) sy - 1, (int) sx + half, (int) sy + 1, col);
                ctx.fill((int) sx - 1, (int) sy - half, (int) sx + 1, (int) sy + half, col);
            } else if (mode.is("Hearts")) {
                // Draw small heart-like shape
                ctx.fill((int) sx - half, (int) sy - half / 2, (int) sx + half, (int) sy + half, col);
                ctx.fill((int) sx - half / 2, (int) sy - half, (int) sx + half / 2, (int) sy + half, col);
            } else {
                // Default: square particle
                ctx.fill((int) sx - half, (int) sy - half, (int) sx + half, (int) sy + half, col);
            }
        }
    }

    @Override
    protected void onDisable() {
        particles.clear();
    }

    private static class HitParticle {
        double x, y, z;
        double vx, vy, vz;
        int color;
        int maxLife;
        int age;

        HitParticle(double x, double y, double z, double vx, double vy, double vz, int color, int maxLife) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.color = color;
            this.maxLife = maxLife;
            this.age = 0;
        }

        void tick() {
            x += vx;
            y += vy;
            z += vz;
            vy -= 0.005; // Gravity
            vx *= 0.96;
            vz *= 0.96;
            age++;
        }

        boolean isDead() {
            return age >= maxLife;
        }

        float getAlpha() {
            return Math.max(0, 1.0f - (float) age / maxLife);
        }
    }
}
