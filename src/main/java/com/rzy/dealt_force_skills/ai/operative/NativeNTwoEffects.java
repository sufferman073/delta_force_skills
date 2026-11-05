package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruExplosionHelper;
import com.rzy.dealt_force_skills.entity.NTwoIceFieldEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared native cold payload; snapshot ownership survives the caster's chunk unload. */
public final class NativeNTwoEffects {
    private NativeNTwoEffects() { }
    public static void apply(Entity source, OperativeAllegiance allegiance, Vec3 center, double radius, int cold, float damage) {
        if (!(source.level() instanceof ServerLevel level)) return;
        LivingEntity owner = allegiance.caster() != null && level.getEntity(allegiance.caster()) instanceof LivingEntity living && living.isAlive() ? living : null;
        for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), LivingEntity::isAlive)) {
            boolean self = allegiance.isCaster(target);
            if ((!self && !allegiance.isEnemy(level, target))
                    || target.getBoundingBox().getCenter().distanceToSqr(center) > radius * radius
                    || !UluruExplosionHelper.hasExplosionLineOfSight(level, center, target)) continue;
            if (!self && allegiance.caster() != null) NTwoStateManager.addNativeCold(allegiance.caster(), owner, target, cold);
            if (damage > 0) {
                Vec3 before = target.getDeltaMovement(); target.invulnerableTime = 0;
                SkillDamageHelper.hurt(target, SkillDamageHelper.trueDamage(level, source, owner), owner, damage);
                target.setDeltaMovement(before); target.hurtMarked = true;
            }
        }
    }
    public static void field(Entity source, OperativeAllegiance allegiance, Vec3 center, boolean condensed) {
        if (!(source.level() instanceof ServerLevel level)) return;
        var field = new NTwoIceFieldEntity(ModEntities.N_TWO_ICE_FIELD.get(), level);
        field.configureNative(allegiance, condensed); field.setPos(center); level.addFreshEntity(field);
    }
}
