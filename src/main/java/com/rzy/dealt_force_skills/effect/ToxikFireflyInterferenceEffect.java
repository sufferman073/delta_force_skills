package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.advancement.DfsAchievements;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SuppressLocalHurtAnimation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public class ToxikFireflyInterferenceEffect extends MobEffect {
    private static final String OWNER_TAG = DealtForceSkillsMod.MODID + ".toxik_firefly_owner";
    private static final String BASE_MAX_HEALTH_TAG = DealtForceSkillsMod.MODID + ".toxik_firefly_base_max_health";
    private static final String REDUCED_HEALTH_TAG = DealtForceSkillsMod.MODID + ".toxik_firefly_reduced_health";
    private static final ResourceLocation SLOW_ID = ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "effect.toxik_firefly_interference.movement_speed");
    private static final ResourceLocation MAX_HEALTH_MODIFIER_UUID = ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "toxik_firefly_interference_effect.max_health_modifier");
    private static final String MAX_HEALTH_MODIFIER_NAME = "Toxik firefly temporary max health reduction";
    private static volatile double MAX_REDUCTION_RATIO = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("MAX_REDUCTION_RATIO", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("effects.toxikfireflyinterferenceeffect.max_reduction_ratio", 0.9999));
    private static volatile double PER_LEVEL_REDUCTION_PER_SECOND = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("PER_LEVEL_REDUCTION_PER_SECOND", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(
      "effects.toxikfireflyinterferenceeffect.per_level_reduction_per_second", 0.01
   ));
    private static volatile int REDUCTION_INTERVAL_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("REDUCTION_INTERVAL_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("effects.toxikfireflyinterferenceeffect.reduction_interval_ticks", 5));
    public ToxikFireflyInterferenceEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                SLOW_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> -0.5D);
    }

    public static void setOwner(LivingEntity target, LivingEntity owner) {
        if (owner != null) {
            target.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        }
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
            return false;
        }
        applyStackingHealthReduction(level, entity, amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % REDUCTION_INTERVAL_TICKS == 0;
    }

    public static void clearCaps(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        double reduced = tag.getDouble(REDUCED_HEALTH_TAG);
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(MAX_HEALTH_MODIFIER_UUID);
        }
        if (reduced > 0.0D && entity.isAlive()) {
            float restored = (float) Math.min(entity.getMaxHealth(), entity.getHealth() + reduced);
            entity.setHealth(Math.max(0.01F, restored));
        }
        tag.remove(BASE_MAX_HEALTH_TAG);
        tag.remove(REDUCED_HEALTH_TAG);
        tag.remove(OWNER_TAG);
    }

    private static void applyStackingHealthReduction(ServerLevel level, LivingEntity entity, int amplifier) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        double alreadyReduced = Math.max(0.0D, tag.getDouble(REDUCED_HEALTH_TAG));
        double baseMaxHealth;
        if (tag.contains(BASE_MAX_HEALTH_TAG)) {
            baseMaxHealth = Math.max(1.0D, tag.getDouble(BASE_MAX_HEALTH_TAG));
        } else {
            baseMaxHealth = Math.max(1.0D, entity.getMaxHealth() + alreadyReduced);
            tag.putDouble(BASE_MAX_HEALTH_TAG, baseMaxHealth);
        }

        double maxReduction = Math.max(0.0D, baseMaxHealth * MAX_REDUCTION_RATIO);
        double reductionThisInterval = baseMaxHealth
                * PER_LEVEL_REDUCTION_PER_SECOND
                * (amplifier + 1)
                * (REDUCTION_INTERVAL_TICKS / 20.0D);
        double newReduced = Math.min(maxReduction, alreadyReduced + reductionThisInterval);
        double delta = Math.max(0.0D, newReduced - alreadyReduced);
        if (delta <= 0.0D) {
            return;
        }

        maxHealth.removeModifier(MAX_HEALTH_MODIFIER_UUID);
        maxHealth.addTransientModifier(new AttributeModifier(
                MAX_HEALTH_MODIFIER_UUID,
                -newReduced,
                AttributeModifier.Operation.ADD_VALUE));
        tag.putDouble(REDUCED_HEALTH_TAG, newReduced);
        owner(level, entity).ifPresent(owner ->
                DfsAchievements.recordToxikFireflyMaxHealthLoss(owner, entity, newReduced / baseMaxHealth));

        float newHealth = (float) Math.max(0.01D, entity.getHealth() - delta);
        float cappedHealth = Math.min(newHealth, entity.getMaxHealth());
        entity.setHealth(cappedHealth);
        if (entity instanceof ServerPlayer player) {
            NetworkHandler.sendToPlayer(new S2C_SuppressLocalHurtAnimation(3), player);
        }
    }

    public static Optional<ServerPlayer> owner(ServerLevel level, LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.hasUUID(OWNER_TAG)) {
            return Optional.empty();
        }
        UUID ownerId = tag.getUUID(OWNER_TAG);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerId);
        return Optional.ofNullable(player);
    }
}
