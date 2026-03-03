package me.totalchaos01.chaosclient.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/**
 * SelectWorldScreen mixin — visual theming is handled by MixinScreen.
 * This class exists as a placeholder for any future SelectWorldScreen-specific injections.
 */
@Mixin(SelectWorldScreen.class)
public abstract class MixinSelectWorldScreen extends Screen {
    protected MixinSelectWorldScreen(Text title) {
        super(title);
    }
}
