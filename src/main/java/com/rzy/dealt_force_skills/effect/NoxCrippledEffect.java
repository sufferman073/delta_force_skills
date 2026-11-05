package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;
import java.util.UUID;

public class NoxCrippledEffect extends MobEffect {
    private static final String OWNER_TAG = DealtForceSkillsMod.MODID + ".nox_crippled_owner";

    public NoxCrippledEffect() {
        super(MobEffectCategory.HARMFUL, 0x20242B);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "61d3071d-f26a-42f4-a0b4-f7f68c9d6a34",
                -0.4D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static final String NATIVE_TAG = "dealt_force_skills.nox_crippled_native";
    public static void setNativeSource(LivingEntity target, com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance allegiance) {
        var tag = new CompoundTag(); allegiance.save(tag, 0);
        target.getPersistentData().put(NATIVE_TAG, tag);
    }
    private static boolean mayDamage(ServerLevel level, LivingEntity target) {
        if (!target.getPersistentData().contains(NATIVE_TAG, 10)) return true;
        var source = new com.rzy.dealt_force_skills.ai.operative.OperativeAllegiance();
        source.load(target.getPersistentData().getCompound(NATIVE_TAG), 1);
        return source.isCaster(target) || source.isEnemy(level, target);
    }

    public static void setOwner(LivingEntity target, LivingEntity owner) {
        target.getPersistentData().remove(NATIVE_TAG);
        if (owner != null) {
            target.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        }
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
            return;
        }

        if (!mayDamage(level, entity)) return;
        LivingEntity owner = owner(level, entity).orElse(null);
        entity.invulnerableTime = 0;
        SkillDamageHelper.hurt(entity, SkillDamageHelper.noxRotor(level, null, owner), owner, com.rzy.dealt_force_skills.config.DealtForceConfig.floatValue("effects.nox_crippled_effect.skill_hurt.0.damage", 8.0f));
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    private static Optional<LivingEntity> owner(ServerLevel level, LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.hasUUID(OWNER_TAG)) {
            return Optional.empty();
        }
        UUID ownerId = tag.getUUID(OWNER_TAG);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerId);
        if (player != null) return Optional.of(player);
        if (tag.contains(NATIVE_TAG, 10) && level.getEntity(ownerId) instanceof LivingEntity living) return Optional.of(living);
        return Optional.empty();
    }
}
