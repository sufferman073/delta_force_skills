package com.rzy.dealt_force_skills.stamina;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class StaminaEvents {
    private StaminaEvents() { }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StaminaMovement.tick(player);
            StaminaManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        StaminaMovement.reset(event.getEntity());
        StaminaManager.sync(event.getEntity());
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            event.getEntity().getPersistentData().put(StaminaManager.ROOT, StaminaManager.data(event.getOriginal()).copy());
        }
        StaminaMovement.reset(event.getEntity());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        StaminaMovement.reset(event.getEntity());
        StaminaManager.sync(event.getEntity());
    }
}
