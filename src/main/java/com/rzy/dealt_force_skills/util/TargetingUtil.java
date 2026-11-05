package com.rzy.dealt_force_skills.util;

import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Shared hostile / friendly targeting helpers.
 * <p>
 * For skills that must hurt or CC the caster (grenades, fields, traps, etc.),
 * use {@link #isSelfOrHostileLivingFor(Entity, LivingEntity)} /
 * {@link #isSelfOrHostilePlayerFor(Entity, Player)}. Teammates stay protected.
 * Design inventory:
 * {@code characters_records/self_harm_skills.txt}
 */
public final class TargetingUtil {
    private TargetingUtil() {
    }

    public static boolean isTargetablePlayer(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.getAbilities().instabuild
                && !DepartmentOfTransportationStateManager.isFullyConcealed(player)
                && (!(player instanceof ServerPlayer serverPlayer)
                || !UndeadStateManager.blocksIncomingAttack(serverPlayer));
    }

    public static boolean isTargetableLiving(LivingEntity entity) {
        return entity.isAlive() && (!(entity instanceof Player player) || isTargetablePlayer(player))
                && (!(entity instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot)
                || !(bot.skills() instanceof com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills department) || !department.concealed(bot));
    }

    /**
     * Hostile-only: skips self and teammates.
     * Prefer {@link #isSelfOrHostileLivingFor} when the skill is listed as self-harming.
     */
    public static boolean isHostilePlayerFor(Entity owner, Player target) {
        return isTargetablePlayer(target) && !shouldSkipFriendlyControl(owner, target);
    }

    /**
     * Hostile-only: skips self and teammates.
     * Prefer {@link #isSelfOrHostileLivingFor} when the skill is listed as self-harming.
     */
    public static boolean isHostileLivingFor(Entity owner, LivingEntity target) {
        return isTargetableLiving(target) && !shouldSkipFactionFriendly(owner, target)
                && !shouldSkipFriendlyControl(owner, target)
                && !isPlayerOwnedSummonOf(owner, target);
    }

    /**
     * Central self-harm targeting: allows the caster and hostiles, blocks teammates.
     * Use for skills that can damage/control the user (see characters_records list).
     */
    public static boolean isSelfOrHostilePlayerFor(Entity owner, Player target) {
        if (isCommanderSource(owner)) {
            return isHostilePlayerFor(owner, target);
        }
        return isTargetablePlayer(target)
                && (isSelf(owner, target) || !shouldSkipFriendlyExceptSelf(owner, target));
    }

    /**
     * Central self-harm targeting: allows the caster and hostiles, blocks teammates.
     * Use for skills that can damage/control the user (see characters_records list).
     */
    public static boolean isSelfOrHostileLivingFor(Entity owner, LivingEntity target) {
        if (isCommanderSource(owner)) {
            return isHostileLivingFor(owner, target);
        }
        return isTargetableLiving(target) && !shouldSkipFactionFriendly(owner, target)
                && (isSelf(owner, target) || (!shouldSkipFriendlyExceptSelf(owner, target)
                && !isPlayerOwnedSummonOf(owner, target)));
    }

    private static boolean shouldSkipFactionFriendly(Entity owner, LivingEntity target) {
        return owner instanceof AcceleratedTroopsEntity && AcceleratedTroopsEntity.isHvkAlly(target);
    }

    public static boolean isCommanderSource(Entity source) {
        if (source instanceof Projectile projectile) return isCommanderSource(projectile.getOwner());
        return source instanceof Player player && CommanderStateManager.isCommander(player)
                || source instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost bot
                && bot.skills() instanceof com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills;
    }

    public static boolean isSelfSource(Entity source, Entity target) {
        if (source instanceof Projectile projectile) return isSelfSource(projectile.getOwner(), target);
        return isSelf(source, target);
    }

    public static boolean isSelf(Entity owner, Entity target) {
        return owner != null && target != null && owner.getUUID().equals(target.getUUID());
    }

    /**
     * External companion mods normally expose Forge's {@link OwnableEntity} contract. Treat an
     * entity owned by the casting player as friendly even when it is not part of a Dealt Force team.
     */
    public static boolean isPlayerOwnedSummonOf(Entity owner, Entity target) {
        if (isSelf(owner, target) || !(target instanceof OwnableEntity ownable)) {
            return false;
        }
        java.util.UUID playerOwnerId = playerOwnerId(owner);
        java.util.UUID ownerId = ownable.getOwnerUUID();
        return playerOwnerId != null && playerOwnerId.equals(ownerId);
    }

    private static java.util.UUID playerOwnerId(Entity entity) {
        if (com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.nativeSummon(entity)) {
            var tag = entity.getPersistentData().getCompound(com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.SUMMON);
            return tag.hasUUID("OperativeOwner") ? tag.getUUID("OperativeOwner") : null;
        }
        if (entity instanceof com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource source
                && source.operativeOwnerUUID() != null) {
            return source.operativeOwnerUUID();
        }
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return ownable.getOwnerUUID();
        }
        if (entity instanceof Projectile projectile) {
            return playerOwnerId(projectile.getOwner());
        }
        return null;
    }

    /**
     * True when player-sourced harmful control should skip the target (self or teammate).
     * For self-damaging skills, do not use this alone - use {@link #shouldSkipFriendlyExceptSelf}.
     */
    public static boolean shouldSkipFriendlyControl(Entity owner, LivingEntity target) {
        if (owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost && isSelf(owner, target)
                || areOperativeAllies(owner, target)) {
            return true;
        }
        if (!(owner instanceof Player) || !(target instanceof Player)) {
            return false;
        }
        return owner.getUUID().equals(target.getUUID()) || DealtTeamManager.areTeammates(owner, target);
    }

    /**
     * Team-safe filter for self-harming skills: only teammates are skipped; self is allowed.
     */
    public static boolean shouldSkipFriendlyExceptSelf(Entity owner, LivingEntity target) {
        if (isSelf(owner, target)) return false;
        if (areOperativeAllies(owner, target)) return true;
        if (!(owner instanceof Player) || !(target instanceof Player)) {
            return false;
        }
        if (owner.getUUID().equals(target.getUUID())) {
            return false;
        }
        return DealtTeamManager.areTeammates(owner, target);
    }

    public static boolean areOperativeAllies(Entity owner, LivingEntity target) {
        if (owner instanceof Projectile projectile) return areOperativeAllies(projectile.getOwner(), target);
        if (owner instanceof com.rzy.dealt_force_skills.entity.OperativeEntity firstBot
                && target instanceof com.rzy.dealt_force_skills.entity.OperativeEntity secondBot
                && firstBot.hostileTo() != null && firstBot.hostileTo().equals(secondBot.hostileTo())) return true;
        if (owner instanceof com.rzy.dealt_force_skills.ai.operative.NativeSaeedGuardEntity guard && guard.related(target)
                || target instanceof com.rzy.dealt_force_skills.ai.operative.NativeSaeedGuardEntity otherGuard && otherGuard.related(owner)) return true;
        boolean nativeOwner = com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.nativeSummon(owner);
        boolean nativeTarget = com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.nativeSummon(target);
        if (nativeOwner || nativeTarget) {
            Entity summon = nativeOwner ? owner : target;
            Entity other = nativeOwner ? target : owner;
            var tag = summon.getPersistentData().getCompound(com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.SUMMON);
            if (other != null && tag.hasUUID("OperativeCaster")) {
                java.util.UUID caster = tag.getUUID("OperativeCaster");
                if (caster.equals(other.getUUID()) || com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.nativeSummon(other)
                        && other.getPersistentData().getCompound(com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.SUMMON).hasUUID("OperativeCaster")
                        && caster.equals(other.getPersistentData().getCompound(com.rzy.dealt_force_skills.ai.operative.NativeDepartmentSkills.SUMMON).getUUID("OperativeCaster"))) return true;
            }
        }
        // Restrict the additional ownership rules to the native-operative feature.
        if (!(owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost)
                && !(owner instanceof com.rzy.dealt_force_skills.ai.operative.OperativeSkillSource)
                && !(target instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost) && !nativeOwner && !nativeTarget) return false;
        java.util.UUID first = playerOwnerId(owner);
        java.util.UUID second = playerOwnerId(target);
        if (first == null || second == null) return false;
        if (first.equals(second)) return true;
        if (owner != null && DealtTeamManager.areTeammatesById(first, second, owner.getServer())) return true;
        Player firstPlayer = target.level().getPlayerByUUID(first);
        Player secondPlayer = target.level().getPlayerByUUID(second);
        return firstPlayer != null && secondPlayer != null && firstPlayer.isAlliedTo(secondPlayer);
    }
}
