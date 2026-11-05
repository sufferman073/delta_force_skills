package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
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
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class SpecialOperationsBaseEffects {
    private static final UUID MOVEMENT_ID = UUID.fromString("bf07bea6-93d3-49b2-9563-2c621b6e2741");
    private static final UUID SWIM_ID = UUID.fromString("0cd21a56-ab87-4779-93db-440ade722768");
    private static final String LAST_AIR = DealtForceSkillsMod.MODID + ".base.last_air";

    private SpecialOperationsBaseEffects() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
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
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (!player.level().isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            tickAttributesAndAir(serverPlayer);


        }
    }

    /** Called after Added listeners, before vanilla merges the incoming effect. */
    public static boolean prepareIncomingEffect(net.minecraft.world.entity.LivingEntity target, MobEffectInstance original) {
        if (!(target instanceof ServerPlayer player) || target.getEffect(original.getEffect()) == original) return true;
        MobEffect effect = original.getEffect();
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
        applyModifier(player.getAttribute(ForgeMod.SWIM_SPEED.get()), SWIM_ID,
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

    private static void applyModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null) {
            return;
        }
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && Math.abs(old.getAmount() - amount) < 0.000001D) {
            return;
        }
        if (old != null) {
            attribute.removeModifier(old);
        }
        if (amount > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
