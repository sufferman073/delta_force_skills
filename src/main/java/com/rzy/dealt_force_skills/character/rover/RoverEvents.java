package com.rzy.dealt_force_skills.character.rover;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.RoverFieldEntity;
import com.rzy.dealt_force_skills.entity.RoverWolfEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class RoverEvents {
    private RoverEvents() {}

    @SubscribeEvent public static void tickPlayer(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) RoverStateManager.tick(player);
    }

    @SubscribeEvent public static void tickLiving(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.LivingEntity living && !living.level().isClientSide) RoverStateManager.tickHealth(living);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void damage(LivingIncomingDamageEvent event) {
        if (RoverStateManager.executingDeath(event.getEntity())) return;
        if (event.getEntity().hasEffect(ModEffects.ROVER_VULNERABLE))
            event.setAmount(event.getAmount() * (float) (1 + Math.max(0, RoverConfig.SMOKE_VULNERABILITY)));
        if (RoverStateManager.isDowned(event.getEntity()))
            event.setAmount(event.getAmount() * (float) Math.max(0, 1 - RoverConfig.DOWNED_DAMAGE_REDUCTION));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void fatalDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof RoverWolfEntity wolf
                && event.getNewDamage() >= wolf.getHealth() && wolf.preventLethalDamage()) {
            event.setNewDamage(0);
            return;
        }
        if (event.getNewDamage() > 0 && event.getNewDamage() >= event.getEntity().getHealth()
                && RoverStateManager.preventDeath(event.getEntity(), event.getSource())) event.setNewDamage(0);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof RoverWolfEntity wolf && wolf.preventLethalDamage()) {
            event.setCanceled(true);
            return;
        }
        if (RoverStateManager.preventDeath(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void deathCleanup(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RoverStateManager.cleanup(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void effect(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModEffects.ROVER_DOWNED) event.setResult(MobEffectEvent.Applicable.Result.APPLY);
        else if (event.getEntity().hasEffect(ModEffects.ROVER_PURIFIED)
                && event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void removeEffect(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModEffects.ROVER_DOWNED
                && RoverStateManager.data(event.getEntity()).getLong("DownedUntil") > 0
                && !RoverStateManager.executingDeath(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void target(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() == null) return;
        for (RoverFieldEntity field : event.getEntity().level().getEntitiesOfClass(RoverFieldEntity.class,
                event.getEntity().getBoundingBox().minmax(event.getNewAboutToBeSetTarget().getBoundingBox()).inflate(RoverConfig.SMOKE_RADIUS))) {
            if (field.conceals(event.getNewAboutToBeSetTarget()) && field.blocksSight(event.getEntity().getEyePosition(), event.getNewAboutToBeSetTarget().getEyePosition())) {
                event.setCanceled(true); return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void attack(AttackEntityEvent event) {
        if (RoverStateManager.isDowned(event.getEntity())) event.setCanceled(true);
    }

    private static void interact(PlayerInteractEvent event) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent cancellable && RoverStateManager.isDowned(event.getEntity())) cancellable.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void useItem(PlayerInteractEvent.RightClickItem event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void useBlock(PlayerInteractEvent.RightClickBlock event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void breakBlock(PlayerInteractEvent.LeftClickBlock event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void useEntity(PlayerInteractEvent.EntityInteract event) { interact(event); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void useEntityAt(PlayerInteractEvent.EntityInteractSpecific event) { interact(event); }

    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RoverStateManager.finishDog(player);
            RoverStateManager.data(player).putInt("Tool", 0);
            RoverStateManager.sync(player);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var wolf = RoverStateManager.dog(player);
            if (wolf != null) wolf.discard();
            RoverStateManager.finishDog(player);
            RoverStateManager.data(player).putInt("Tool", 0);
            RoverStateManager.data(player).putInt("RescueTicks", 0);
            RoverStateManager.data(player).remove("RescueTarget");
            com.rzy.dealt_force_skills.compat.PlayerReviveCompat.clearRoverBleeding(player);
        }
    }
}
