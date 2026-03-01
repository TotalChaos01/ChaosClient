package me.totalchaos01.chaosclient.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to expose private methods of MinecraftClient.
 */
@Mixin(MinecraftClient.class)
public interface IMinecraftClientAccessor {

    @Invoker("doAttack")
    boolean invokeDoAttack();
}
