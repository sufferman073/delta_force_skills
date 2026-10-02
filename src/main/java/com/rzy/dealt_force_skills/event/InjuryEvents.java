package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.effect.InjuryManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class InjuryEvents {
    private InjuryEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            InjuryManager.onDamage(player, event.getSource(),
                    CommonEvents.estimateEquipmentHitHeightRatio(player, event.getSource()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFall(LivingFallEvent event) {
        if (!event.isCanceled() && event.getDistance() > 3.0F && event.getEntity() instanceof ServerPlayer player) {
            InjuryManager.onFall(player);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            InjuryManager.onJump(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (InjuryManager.shouldPreventSprinting(event.getEntity())) {
            event.getEntity().setSprinting(false);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            InjuryManager.tick(player);
        }
    }
}
