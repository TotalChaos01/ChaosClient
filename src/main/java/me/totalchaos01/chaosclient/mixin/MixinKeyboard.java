package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.events.EventKey;
import me.totalchaos01.chaosclient.module.Module;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int action, KeyInput keyInput, CallbackInfo ci) {
        if (ChaosClient.getInstance() == null) return;
        if (MinecraftClient.getInstance().currentScreen != null) return;
        if (action != GLFW.GLFW_PRESS) return;

        int key = keyInput.key();
        int scanCode = keyInput.scancode();

        // Toggle modules by keybind
        for (Module module : ChaosClient.getInstance().getModuleManager().getModules()) {
            if (module.getKeyBind() == key) {
                module.toggle();
            }
        }

        // Post key event
        ChaosClient.getInstance().getEventBus().post(new EventKey(key, scanCode));
    }
}
