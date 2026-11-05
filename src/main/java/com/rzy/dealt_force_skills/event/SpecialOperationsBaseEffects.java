package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class SpecialOperationsBaseEffects {
    private static final ResourceLocation MOVEMENT_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "base_training_movement");
    private static final ResourceLocation SWIM_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "base_diving_swim");
    private static final String LAST_AIR = DealtForceSkillsMod.MODID + ".base.last_air";

    private SpecialOperationsBaseEffects() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.SHOOTING_RANGE);
        if (level > 0) {
            event.setAmount(event.getAmount() * Math.max(0.0F, 1.0F - level * 0.03F));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(BreakSpeed event) {
        int level = SpecialOperationsBaseManager.effectiveLevel(event.getEntity(),
                SpecialOperationsBaseManager.Facility.SHOOTING_RANGE);
        if (level > 0 && event.getNewSpeed() > 0.0F) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + level * 0.05F));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.SHOOTING_RANGE);
        if (level > 0 && event.getDuration() > 1 && player.level().getGameTime() % 20L < level) {
            event.setDuration(event.getDuration() - 1);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (event.getAmount() <= 0) {
            return;
        }
        int level = SpecialOperationsBaseManager.effectiveLevel(event.getEntity(),
                SpecialOperationsBaseManager.Facility.COMMAND_CENTER);
        if (level > 0) {
            event.setAmount(Math.max(1, Math.round(event.getAmount() * (1.0F + level * 0.2F))));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            tickAttributesAndAir(serverPlayer);


        }
    }

    /** Called after Added listeners, before vanilla merges the incoming effect. */
    public static boolean prepareIncomingEffect(net.minecraft.world.entity.LivingEntity target, MobEffectInstance original) {
        if (!(target instanceof ServerPlayer player) || target.getEffect(original.getEffect()) == original) return true;
        MobEffect effect = original.getEffect().value();
        if (effect.isInstantenous() || effect.getCategory() == MobEffectCategory.NEUTRAL) return true;
        int level = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.DECONTAMINATION_ROOM);
        if (level <= 0) return true;
        int amplifier;
        int duration;
        if (effect.getCategory() == MobEffectCategory.HARMFUL) {
            amplifier = original.getAmplifier() - level;
            // The room cleanses the incoming effect; it must not erase a stronger active instance.
            if (amplifier < 0) return false;
            duration = scaleDuration(original.getDuration(), Math.max(0.25F, 1.0F - level * 0.25F));
        } else {
            amplifier = (int) Math.min(Integer.MAX_VALUE, (long) original.getAmplifier() + (level >= 3 ? 1 : 0));
            duration = scaleDuration(original.getDuration(), 1.0F + level * 0.3F);
        }
        var mutable = (com.rzy.dealt_force_skills.mixin.OperativeEffectAccessor) original;
        mutable.dealt_force_skills$setDuration(duration);
        mutable.dealt_force_skills$setAmplifier(amplifier);
        return true;
    }

    private static int scaleDuration(int duration, float multiplier) {
        return duration < 0 ? duration : Math.max(1, Math.round(duration * multiplier));
    }

    private static void tickAttributesAndAir(ServerPlayer player) {
        int training = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.TRAINING_CENTER);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_ID,
                "Special Operations Base training", training * 0.03D);
        int diving = SpecialOperationsBaseManager.effectiveLevel(player,
                SpecialOperationsBaseManager.Facility.DIVING_CENTER);
        applyModifier(player.getAttribute(NeoForgeMod.SWIM_SPEED), SWIM_ID,
                "Special Operations Base swimming", diving * 0.25D);

        int currentAir = player.getAirSupply();
        int previousAir = player.getPersistentData().contains(LAST_AIR)
                ? player.getPersistentData().getInt(LAST_AIR) : currentAir;
        if (diving > 0 && currentAir < previousAir
                && player.level().getGameTime() % 10L < diving * 3L) {
            player.setAirSupply(Math.min(player.getMaxAirSupply(), currentAir + (previousAir - currentAir)));
            currentAir = player.getAirSupply();
        }
        player.getPersistentData().putInt(LAST_AIR, currentAir);
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id, String name, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && Math.abs(old.amount() - amount) < 0.000001D) {
            return;
        }
        attribute.removeModifier(id);
        if (amount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
