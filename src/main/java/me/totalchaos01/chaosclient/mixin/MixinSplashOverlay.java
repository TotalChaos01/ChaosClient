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

        int backColor = (alpha << 24) | 0x0a0a0f;
        context.fill(0, 0, width, height, backColor);

        float r = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + r * 0.050000012F, 0.0F, 1.0F);

        if (f < 1.0F) {
            float opacity = 1.0F - MathHelper.clamp(f, 0.0F, 1.0F);
            int barAlpha = Math.round(opacity * 255.0F);

            int barW = 200;
            int barH = 5;
            int barX = width / 2 - barW / 2;
            int barY = height / 2 + 25;
            
            context.fill(barX, barY, barX + barW, barY + barH, (barAlpha << 24) | 0x000000);
            
            int p = (int) (barW * this.progress);
            context.fill(barX, barY, barX + p, barY + barH, (barAlpha << 24) | 0xA855F7);
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
