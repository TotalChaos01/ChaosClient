package me.totalchaos01.chaosclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to expose private methods/fields of MinecraftClient.
 */
@Mixin(MinecraftClient.class)
public interface IMinecraftClientAccessor {

    @Invoker("doAttack")
    boolean invokeDoAttack();

    @Mutable
    @Accessor("session")
    void setSession(Session session);

    @Accessor("itemUseCooldown")
    int getItemUseCooldown();

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int cooldown);
}
