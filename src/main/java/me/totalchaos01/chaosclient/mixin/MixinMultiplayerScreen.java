package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ui.network.AltManagerScreen;
import me.totalchaos01.chaosclient.ui.network.ViaVersionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds AltManager and ViaVersion buttons to the multiplayer screen.
 * Visual theming is handled by MixinScreen.
 */
@Mixin(MultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {

    protected MixinMultiplayerScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int btnW = 80;
        int gap = 4;
        int x = this.width - btnW * 2 - gap - 6;
        int y = 4;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("AltManager"),
                btn -> client.setScreen(new AltManagerScreen((MultiplayerScreen)(Object)this))
        ).dimensions(x, y, btnW, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("ViaVersion"),
                btn -> client.setScreen(new ViaVersionScreen((MultiplayerScreen)(Object)this))
        ).dimensions(x + btnW + gap, y, btnW, 20).build());
    }
}
