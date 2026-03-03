package me.totalchaos01.chaosclient.module.impl.player;

import me.totalchaos01.chaosclient.event.EventTarget;
import me.totalchaos01.chaosclient.event.events.EventTick;
import me.totalchaos01.chaosclient.module.Category;
import me.totalchaos01.chaosclient.module.Module;
import me.totalchaos01.chaosclient.module.ModuleInfo;
import me.totalchaos01.chaosclient.setting.impl.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * FakePlayer — spawns a client-side fake player entity for testing purposes.
 */
@ModuleInfo(name = "FakePlayer", description = "Spawns a fake player for testing", category = Category.PLAYER)
public class FakePlayer extends Module {

    private final BooleanSetting copyInventory = new BooleanSetting("Copy Inventory", true);

    private net.minecraft.client.network.OtherClientPlayerEntity fakePlayer;

    public FakePlayer() {
        addSettings(copyInventory);
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        fakePlayer = new net.minecraft.client.network.OtherClientPlayerEntity(
            mc.world, mc.player.getGameProfile()
        );

        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHeadYaw(mc.player.getHeadYaw());
        fakePlayer.setBodyYaw(mc.player.getBodyYaw());

        // Copy health
        fakePlayer.setHealth(mc.player.getHealth());

        mc.world.addEntity(fakePlayer);
    }

    @Override
    protected void onDisable() {
        if (fakePlayer != null && mc.world != null) {
            fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }

    public Entity getFakePlayer() {
        return fakePlayer;
    }
}
