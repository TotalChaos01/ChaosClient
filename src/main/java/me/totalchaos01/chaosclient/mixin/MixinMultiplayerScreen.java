package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ui.network.AltManagerScreen;
import me.totalchaos01.chaosclient.ui.network.ViaVersionScreen;
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
        this.addDrawableChild(ButtonWidget.builder(Text.literal("AltManager"), button -> {
            this.client.setScreen(new AltManagerScreen((Screen) (Object) this));
        }).dimensions(8, 8, 110, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("ViaVersion"), button -> {
            this.client.setScreen(new ViaVersionScreen((Screen) (Object) this));
        }).dimensions(124, 8, 100, 20).build());
    }

}
