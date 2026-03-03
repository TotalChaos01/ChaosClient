package me.totalchaos01.chaosclient.mixin;

import me.totalchaos01.chaosclient.ChaosClient;
import me.totalchaos01.chaosclient.event.events.EventChatSend;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayerEntity {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (ChaosClient.getInstance() == null) return;

        // Handle client command prefix
        if (message.startsWith(".")) {
            ChaosClient.getInstance().getCommandManager().handle(message.substring(1));
            ci.cancel();
            return;
        }

        // Handle Baritone # prefix — forward via reflection (soft-dependency)
        if (message.startsWith("#")) {
            ci.cancel();
            String baritoneCmd = message.substring(1).trim();
            if (baritoneCmd.isEmpty()) return;
            try {
                Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
                Method getProvider = apiClass.getMethod("getProvider");
                Object provider = getProvider.invoke(null);
                Method getPrimaryBaritone = provider.getClass().getMethod("getPrimaryBaritone");
                Object baritone = getPrimaryBaritone.invoke(provider);
                Method getCommandManager = baritone.getClass().getMethod("getCommandManager");
                Object cmdManager = getCommandManager.invoke(baritone);
                Method execute = cmdManager.getClass().getMethod("execute", String.class);
                execute.invoke(cmdManager, baritoneCmd);
            } catch (ClassNotFoundException e) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    mc.player.sendMessage(
                            Text.literal("\u00a7c[ChaosClient] \u00a77Baritone не установлен! Установите Baritone для использования # команд."),
                            false
                    );
                }
            } catch (Exception e) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    mc.player.sendMessage(
                            Text.literal("\u00a7c[ChaosClient] \u00a77Ошибка Baritone: " + e.getMessage()),
                            false
                    );
                }
            }
            return;
        }

        EventChatSend event = new EventChatSend(message);
        ChaosClient.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
