package com.rzy.dealt_force_skills.effect;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public class RaptorHummingbirdMarkedEffect extends MobEffect {
    private static final String OWNER_TAG = DealtForceSkillsMod.MODID + ".raptor_hummingbird_owner";

    public RaptorHummingbirdMarkedEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFF);
    }

    public static void setOwner(LivingEntity target, ServerPlayer owner) {
        if (owner != null) {
            target.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        }
    }

    public static Optional<ServerPlayer> owner(ServerLevel level, LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.hasUUID(OWNER_TAG)) {
            return Optional.empty();
        }
        UUID ownerId = tag.getUUID(OWNER_TAG);
        return Optional.ofNullable(level.getServer().getPlayerList().getPlayer(ownerId));
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        RaptorStateManager.revealFromHummingbird(entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
