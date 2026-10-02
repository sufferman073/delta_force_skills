package com.rzy.dealt_force_skills.team;

import com.rzy.dealt_force_skills.effect.EffectRedirectCategories;
import com.rzy.dealt_force_skills.effect.ShakehandsCombatHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Central team combat rules:
 * <ul>
 *   <li>Same Dealt Force team: no PvP damage, no harmful/control effects on teammates</li>
 *   <li>Different teams: beneficial effects cannot be applied to enemies</li>
 *   <li>Players with no team are neutral: both beneficial and harmful effects are allowed</li>
 * </ul>
 */
public final class TeamCombatRules {
    private TeamCombatRules() {
    }

    public static boolean shouldCancelFriendlyDamage(LivingEntity target, DamageSource source) {
        if (target == null || source == null) {
            return false;
        }
        LivingEntity attacker = ShakehandsCombatHandler.resolveAttacker(source, target);
        return shouldCancelFriendlyDamage(target, attacker);
    }

    public static boolean shouldCancelFriendlyDamage(LivingEntity target, Entity attacker) {
        if (!(target instanceof Player) || !(attacker instanceof Player)) {
            return false;
        }
        if (attacker.getUUID().equals(target.getUUID())) {
            return false;
        }
        return DealtTeamManager.areTeammates(attacker, target);
    }

    /**
     * @return true if a player-sourced effect should not apply to the holder under team rules
     */
    public static boolean shouldBlockPlayerEffect(LivingEntity holder, Entity source, MobEffectInstance instance) {
        if (holder == null || instance == null || !(holder instanceof Player targetPlayer)) {
            return false;
        }
        if (!(source instanceof Player sourcePlayer)) {
            return false;
        }
        if (sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
            return false;
        }

        MobEffect effect = instance.getEffect().value();
        boolean harmful = isHarmfulOrControl(effect);
        boolean beneficial = effect.getCategory() == MobEffectCategory.BENEFICIAL;

        if (DealtTeamManager.areTeammates(sourcePlayer, targetPlayer)) {
            // Teammates: block debuffs/controls; buffs are allowed.
            return harmful;
        }

        // Neutral (either player not on a team): allow both beneficial and harmful.
        if (!DealtTeamManager.isInTeam(sourcePlayer) || !DealtTeamManager.isInTeam(targetPlayer)) {
            return false;
        }

        // Different teams: block beneficial (no buffing enemies); harmful is allowed.
        return beneficial;
    }

    public static boolean isHarmfulOrControl(MobEffect effect) {
        if (effect == null) {
            return false;
        }
        if (effect.getCategory() == MobEffectCategory.HARMFUL) {
            return true;
        }
        return EffectRedirectCategories.isRedirectable(effect);
    }

    public static boolean isTeammateFootprintOwner(ServerPlayer viewer, String ownerUuid) {
        if (viewer == null || ownerUuid == null || ownerUuid.isEmpty()) {
            return false;
        }
        try {
            java.util.UUID uuid = java.util.UUID.fromString(ownerUuid);
            ServerPlayer owner = viewer.server.getPlayerList().getPlayer(uuid);
            if (owner == null) {
                // Offline teammate: check team membership by UUID via manager data.
                return DealtTeamManager.areTeammatesById(viewer.getUUID(), uuid, viewer.server);
            }
            return DealtTeamManager.areTeammates(viewer, owner);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
