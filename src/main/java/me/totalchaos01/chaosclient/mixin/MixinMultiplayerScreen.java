package me.totalchaos01.chaosclient.mixin;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {

    protected MixinMultiplayerScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // AltManager Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("AltManager"), button -> {
            // TODO: Route to simple AltManager implementation
        }).dimensions(5, 5, 100, 20).build());

        // ViaVersion Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("ViaVersion / ViaMCP"), button -> {
            // TODO: Route to ViaVersion UI implementation
        }).dimensions(110, 5, 120, 20).build());
    }
}
