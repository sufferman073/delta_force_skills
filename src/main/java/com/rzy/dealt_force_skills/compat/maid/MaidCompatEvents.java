package com.rzy.dealt_force_skills.compat.maid;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class MaidCompatEvents {
    private MaidCompatEvents() { }
    @SubscribeEvent
    public static void tracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof MaidSkillHost host)
            com.rzy.dealt_force_skills.network.NetworkHandler.sendToPlayer(host.dealtForceMaidSkills().renderPacket(), player);
    }
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof MaidSkillHost host) host.dealtForceMaidSkills().tick();
    }
    // Friendly-fire protection can cancel the owner attack before this listener runs.
    @SubscribeEvent(priority=EventPriority.HIGHEST, receiveCanceled=true)
    public static void attack(AttackEntityEvent event) {
        if (interactMaid(event, event.getTarget())) event.setCanceled(true);
    }
    private static boolean interactMaid(AttackEntityEvent event, net.minecraft.world.entity.Entity entity) {
        if (!event.getEntity().isShiftKeyDown()
                || !(entity instanceof MaidSkillHost host) || !host.dealtForceMaidSkills().supported()) return false;
        var player = event.getEntity();
        if (!(entity instanceof net.minecraft.world.entity.TamableAnimal maid)
                || !player.getUUID().equals(maid.getOwnerUUID())) return false;
        boolean selector = player.getMainHandItem().is(com.rzy.dealt_force_skills.registry.ModItems.DRIFTWOOD.get());
        boolean empty = player.getMainHandItem().isEmpty();
        if (!selector && !empty || host.dealtForceMaidSkills().failed()) return false;
        // Both gestures suppress the attack, including clearing an already unassigned maid.
        if (player.level().isClientSide) return true;
        if (player instanceof ServerPlayer serverPlayer) {
            if (!MaidSelection.owned(serverPlayer, entity)) return false;
            if (selector) MaidSelection.open(serverPlayer, entity);
            else MaidSelection.clear(serverPlayer, entity);
        }
        return true;
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void incoming(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof MaidSkillHost host) {
            float amount = host.dealtForceMaidSkills().incoming(event.getSource(), event.getAmount());
            if (amount <= 0) event.setCanceled(true); else event.setAmount(amount);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damaged(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getAmount() > 0 && !event.getEntity().level().isClientSide && event.getEntity() instanceof MaidSkillHost host)
            host.dealtForceMaidSkills().damaged();
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof MaidSkillHost host
                && host.dealtForceMaidSkills().preventDeath(event.getSource())) event.setCanceled(true);
    }
    @SubscribeEvent
    public static void fall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof MaidSkillHost host)
            event.setDamageMultiplier(host.dealtForceMaidSkills().fall(event.getDistance(), event.getDamageMultiplier()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void finalDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof MaidSkillHost host)
            host.dealtForceMaidSkills().prepareDeath();
    }
}
