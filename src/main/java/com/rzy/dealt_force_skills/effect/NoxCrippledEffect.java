package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public class NoxCrippledEffect extends MobEffect {
   private static final String OWNER_TAG = "dealt_force_skills.nox_crippled_owner";

   public NoxCrippledEffect() {
      super(MobEffectCategory.HARMFUL, 16777215);
      this.addAttributeModifier(
         Attributes.MOVEMENT_SPEED,
         ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "effect.nox_crippled.movement_speed"),
         -0.4,
         Operation.ADD_MULTIPLIED_TOTAL
      );
   }

   public static void setOwner(LivingEntity target, LivingEntity owner) {
      if (owner != null) {
         target.getPersistentData().putUUID("dealt_force_skills.nox_crippled_owner", owner.getUUID());
      }
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      if (entity.level() instanceof ServerLevel level && entity.isAlive()) {
         LivingEntity owner = owner(level, entity).orElse(null);
         entity.invulnerableTime = 0;
         SkillDamageHelper.hurt(
            entity, SkillDamageHelper.noxRotor(level, null, owner), owner, DealtForceConfig.floatValue("effects.nox_crippled_effect.skill_hurt.0.damage", 8.0F)
         );
         return true;
      }

      return false;
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 20 == 0;
   }

   private static Optional<LivingEntity> owner(ServerLevel level, LivingEntity entity) {
      CompoundTag tag = entity.getPersistentData();
      if (!tag.hasUUID("dealt_force_skills.nox_crippled_owner")) {
         return Optional.empty();
      } else {
         UUID ownerId = tag.getUUID("dealt_force_skills.nox_crippled_owner");
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerId);
         return Optional.ofNullable(player);
      }
   }
}
