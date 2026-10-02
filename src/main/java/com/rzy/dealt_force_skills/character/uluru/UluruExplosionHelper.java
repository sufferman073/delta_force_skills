package com.rzy.dealt_force_skills.character.uluru;

import com.rzy.dealt_force_skills.block.QuickCoverBlockEntity;
import com.rzy.dealt_force_skills.entity.UluruBombletEntity;
import com.rzy.dealt_force_skills.entity.UluruFireFieldEntity;
import com.rzy.dealt_force_skills.entity.UluruIncendiaryGrenadeEntity;
import com.rzy.dealt_force_skills.registry.ModBlocks;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class UluruExplosionHelper {
    private UluruExplosionHelper() {
    }

    public static void damageRadius(
            ServerLevel level,
            Vec3 center,
            Entity direct,
            LivingEntity owner,
            double radius,
            float playerDamage,
            float nonPlayerDamage,
            boolean falloff,
            boolean ignite,
            int fireSeconds,
            boolean resetInvulnerability
    ) {
        damageRadius(level, center, direct, owner, radius, playerDamage, nonPlayerDamage,
                falloff, ignite, fireSeconds, resetInvulnerability, false);
    }

    public static void damageRadius(
            ServerLevel level,
            Vec3 center,
            Entity direct,
            LivingEntity owner,
            double radius,
            float playerDamage,
            float nonPlayerDamage,
            boolean falloff,
            boolean ignite,
            int fireSeconds,
            boolean resetInvulnerability,
            boolean preserveVelocity
    ) {
        damageRadius(level, center, direct, owner, radius, playerDamage, nonPlayerDamage,
                falloff, ignite, fireSeconds, resetInvulnerability, preserveVelocity, false);
    }

    public static void damageRadiusIgnoringInvulnerability(
            ServerLevel level,
            Vec3 center,
            Entity direct,
            LivingEntity owner,
            double radius,
            float playerDamage,
            float nonPlayerDamage,
            boolean falloff,
            boolean ignite,
            int fireSeconds,
            boolean preserveVelocity
    ) {
        damageRadius(level, center, direct, owner, radius, playerDamage, nonPlayerDamage,
                falloff, ignite, fireSeconds, true, preserveVelocity, true);
    }

    public static void damageRadiusBlockedByWalls(
            ServerLevel level,
            Vec3 center,
            Entity direct,
            LivingEntity owner,
            double radius,
            float playerDamage,
            float nonPlayerDamage,
            boolean falloff,
            boolean ignite,
            int fireSeconds,
            boolean resetInvulnerability
    ) {
        damageRadius(level, center, direct, owner, radius, playerDamage, nonPlayerDamage,
                falloff, ignite, fireSeconds, resetInvulnerability, false, false, true);
    }

    private static void damageRadius(
            ServerLevel level,
            Vec3 center,
            Entity direct,
            LivingEntity owner,
            double radius,
            float playerDamage,
            float nonPlayerDamage,
            boolean falloff,
            boolean ignite,
            int fireSeconds,
            boolean resetInvulnerability,
            boolean preserveVelocity,
            boolean preserveInvulnerabilityAfterDamage
    ) {
        damageRadius(level, center, direct, owner, radius, playerDamage, nonPlayerDamage,
                falloff, ignite, fireSeconds, resetInvulnerability, preserveVelocity,
                preserveInvulnerabilityAfterDamage, false);
    }

    private static void damageRadius(
            ServerLevel level,
            Vec3 center,
            Entity direct,
            LivingEntity owner,
            double radius,
            float playerDamage,
            float nonPlayerDamage,
            boolean falloff,
            boolean ignite,
            int fireSeconds,
            boolean resetInvulnerability,
            boolean preserveVelocity,
            boolean preserveInvulnerabilityAfterDamage,
            boolean requireLineOfSight
    ) {
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!TargetingUtil.isSelfOrHostileLivingFor(owner, target)) {
                continue;
            }
            double distance = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).distanceTo(center);
            if (distance > radius) {
                continue;
            }
            if (requireLineOfSight && !hasExplosionLineOfSight(level, center, target)) {
                continue;
            }

            float baseDamage = target instanceof Player ? playerDamage : nonPlayerDamage;
            float amount = falloff ? baseDamage * Math.max(0.0f, 1.0f - (float) distance * 0.1f) : baseDamage;
            if (amount <= 0.0f) {
                continue;
            }

            int invulnerableTimeBeforeDamage = target.invulnerableTime;
            if (resetInvulnerability) {
                target.invulnerableTime = 0;
            }
            Vec3 beforeDamageMovement = target.getDeltaMovement();
            boolean damaged = SkillDamageHelper.hurt(target, source(level, direct, owner), owner, amount);
            if (!damaged) {
                if (resetInvulnerability) {
                    target.invulnerableTime = invulnerableTimeBeforeDamage;
                }
                if (preserveVelocity) {
                    target.setDeltaMovement(beforeDamageMovement);
                    target.hurtMarked = true;
                }
                continue;
            }
            if (preserveInvulnerabilityAfterDamage) {
                target.invulnerableTime = invulnerableTimeBeforeDamage;
            }
            if (preserveVelocity) {
                target.setDeltaMovement(beforeDamageMovement);
                target.hurtMarked = true;
            }
            if (ignite) {
                target.igniteForSeconds(fireSeconds);
            }
        }
    }

    public static boolean hasExplosionLineOfSight(ServerLevel level, Vec3 center, LivingEntity target) {
        Vec3 start = center.add(0.0D, 0.1D, 0.0D);
        Vec3 body = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        return hasLineOfSight(level, start, target.getEyePosition(), target)
                || hasLineOfSight(level, start, body, target);
    }

    private static boolean hasLineOfSight(ServerLevel level, Vec3 start, Vec3 end, LivingEntity target) {
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target)).getType()
                == HitResult.Type.MISS;
    }

    public static void destroyQuickCovers(ServerLevel level, Vec3 center, double radius) {
        if (!ModGameRules.areSkillBlockBreaksEnabled(level)) {
            return;
        }
        int minX = (int) Math.floor(center.x - radius);
        int minY = (int) Math.floor(center.y - radius);
        int minZ = (int) Math.floor(center.z - radius);
        int maxX = (int) Math.ceil(center.x + radius);
        int maxY = (int) Math.ceil(center.y + radius);
        int maxZ = (int) Math.ceil(center.z + radius);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (level.getBlockState(pos).is(ModBlocks.QUICK_COVER.get())
                    && Vec3.atCenterOf(pos).distanceTo(center) <= radius + 1.0) {
                QuickCoverBlockEntity.destroyCoverAt(level, pos);
            }
        }
    }

    public static boolean isBreakableGlass(BlockState state) {
        return state.getBlock() instanceof TransparentBlock
                || state.getBlock() instanceof StainedGlassPaneBlock
                || state.is(Blocks.GLASS_PANE);
    }

    private static DamageSource source(ServerLevel level, Entity direct, LivingEntity owner) {
        if (direct instanceof UluruFireFieldEntity) {
            return SkillDamageHelper.uluruFireField(level, direct);
        }
        if (direct instanceof UluruIncendiaryGrenadeEntity) {
            return SkillDamageHelper.uluruIncendiary(level, direct, owner);
        }
        if (direct instanceof UluruBombletEntity) {
            return SkillDamageHelper.uluruMissile(level, direct, owner);
        }
        if (owner instanceof Player player) {
            return player.damageSources().playerAttack(player);
        }
        return level.damageSources().generic();
    }
}
