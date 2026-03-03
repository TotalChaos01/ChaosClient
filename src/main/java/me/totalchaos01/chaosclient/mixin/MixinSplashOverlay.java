package me.totalchaos01.chaosclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.totalchaos01.chaosclient.ChaosClient;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {

    @Shadow @Final private MinecraftClient client;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;
    @Shadow @Final private boolean reloading;
    @Shadow @Final private net.minecraft.resource.ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRenderStart(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        long l = Util.getMeasuringTimeMs();
        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = l;
        }

        float f = this.reloadCompleteTime > -1L ? (float)(l - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float g = this.reloadStartTime > -1L ? (float)(l - this.reloadStartTime) / 500.0F : -1.0F;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        int alpha = 255;
        if (f >= 1.0F) {
            if (this.client.currentScreen != null) {
                this.client.currentScreen.render(context, 0, 0, delta);
            }
            alpha = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
        } else if (this.reloading) {
            if (this.client.currentScreen != null && g < 1.0F) {
                this.client.currentScreen.render(context, mouseX, mouseY, delta);
            }
            alpha = MathHelper.ceil(MathHelper.clamp(g, 0.15F, 1.0F) * 255.0F);
        }

        // Dark terminal background
        int backColor = (alpha << 24) | 0x0a0a0f;
        context.fill(0, 0, width, height, backColor);

        float r = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + r * 0.050000012F, 0.0F, 1.0F);

        if (f < 1.0F) {
            float opacity = 1.0F - MathHelper.clamp(f, 0.0F, 1.0F);
            int textAlpha = Math.round(opacity * 255.0F);
            int green = (textAlpha << 24) | 0x55FF55;     // terminal green
            int purple = (textAlpha << 24) | 0xA855F7;     // theme purple
            int gray = (textAlpha << 24) | 0x888888;       // dim gray
            int white = (textAlpha << 24) | 0xEEEEEE;

            // ─── Client title (big, top area) ───────────────
            var tr = this.client.textRenderer;
            String title = ChaosClient.CLIENT_NAME + " v" + ChaosClient.CLIENT_VERSION;
            context.drawTextWithShadow(tr, title, 16, 12, purple);

            // ─── Linux-style [OK] status lines ──────────────
            String[] stages = {
                "Загрузка ядра клиента",
                "Инициализация EventBus",
                "Загрузка модулей",
                "Инициализация тем",
                "Загрузка конфигурации",
                "Подключение модулей PVP",
                "Инициализация рендера",
                "Загрузка шрифтов",
                "Применение миксинов",
                "Финализация"
            };

            int lineY = 30;
            float progressPer = this.progress / 1.0f;
            int completedStages = (int)(progressPer * stages.length);

            for (int i = 0; i < stages.length; i++) {
                if (i <= completedStages) {
                    // [OK] or [..] status
                    String status;
                    int statusColor;
                    if (i < completedStages) {
                        status = "[ OK ]";
                        statusColor = green;
                    } else {
                        status = "[ .. ]";
                        statusColor = (textAlpha << 24) | 0xFFAA00; // yellow for in-progress
                    }
                    context.drawTextWithShadow(tr, status, 16, lineY, statusColor);
                    context.drawTextWithShadow(tr, stages[i], 62, lineY, white);
                }
                lineY += 11;
            }

            // ─── Hash progress bar at bottom ─────────────────
            int barY = height - 28;
            int barWidth = 30;
            int filled = (int)(barWidth * this.progress);
            int empty = barWidth - filled;

            StringBuilder barStr = new StringBuilder("[");
            for (int i = 0; i < filled; i++) barStr.append('#');
            for (int i = 0; i < empty; i++) barStr.append('-');
            barStr.append("] ").append(String.format("%.0f%%", this.progress * 100));

            context.drawTextWithShadow(tr, barStr.toString(), 16, barY, purple);

            // Loading label
            context.drawTextWithShadow(tr, "Загрузка ChaosClient...", 16, barY - 12, gray);
        }

        if (f >= 2.0F) {
            this.client.setOverlay(null);
        }

        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || g >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.exceptionHandler.accept(Optional.of(throwable));
            }
            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(width, height);
            }
        }
    }
}
