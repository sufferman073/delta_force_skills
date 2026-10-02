package com.rzy.dealt_force_skills.util;

import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameType;

/** Shared hostile/friendly targeting. Use isSelfOrHostile* for self-harm skills. */
public final class TargetingUtil {
    private TargetingUtil() {
    }

    public static boolean isTargetablePlayer(Player player) {
        if (!player.isAlive()
                || player.isSpectator()
                || player.getAbilities().instabuild
                || DepartmentOfTransportationStateManager.isFullyConcealed(player)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return !hasUntargetableGameMode(serverPlayer)
                    && !UndeadStateManager.blocksIncomingAttack(serverPlayer);
        }
        return true;
    }

    public static boolean isTargetableLiving(LivingEntity entity) {
        return entity.isAlive() && (!(entity instanceof Player player) || isTargetablePlayer(player));
    }

    public static boolean isHostilePlayerFor(Entity owner, Player target) {
        return isTargetablePlayer(target) && !shouldSkipFriendlyControl(owner, target);
    }

    public static boolean isHostileLivingFor(Entity owner, LivingEntity target) {
        return isTargetableLiving(target) && !shouldSkipFactionFriendly(owner, target)
                && !shouldSkipFriendlyControl(owner, target)
                && !isPlayerOwnedSummonOf(owner, target);
    }

    public static boolean isSelfOrHostilePlayerFor(Entity owner, Player target) {
        if (owner instanceof Player player && CommanderStateManager.isCommander(player)) {
            return isHostilePlayerFor(owner, target);
        }
        return isTargetablePlayer(target)
                && (isSelf(owner, target) || !shouldSkipFriendlyExceptSelf(owner, target));
    }

    public static boolean isSelfOrHostileLivingFor(Entity owner, LivingEntity target) {
        if (owner instanceof Player player && CommanderStateManager.isCommander(player)) {
            return isHostileLivingFor(owner, target);
        }
        return isTargetableLiving(target) && !shouldSkipFactionFriendly(owner, target)
                && (isSelf(owner, target) || (!shouldSkipFriendlyExceptSelf(owner, target)
                && !isPlayerOwnedSummonOf(owner, target)));
    }

    private static boolean shouldSkipFactionFriendly(Entity owner, LivingEntity target) {
        return owner instanceof AcceleratedTroopsEntity && AcceleratedTroopsEntity.isHvkAlly(target);
    }

    public static boolean isSelf(Entity owner, Entity target) {
        return owner != null && target != null && owner.getUUID().equals(target.getUUID());
    }

    public static boolean isPlayerOwnedSummonOf(Entity owner, Entity target) {
        if (!(target instanceof OwnableEntity ownable)) {
            return false;
        }
        java.util.UUID playerOwnerId = playerOwnerId(owner);
        java.util.UUID ownerId = ownable.getOwnerUUID();
        return playerOwnerId != null && playerOwnerId.equals(ownerId);
    }

    private static java.util.UUID playerOwnerId(Entity entity) {
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

    public static boolean shouldSkipFriendlyControl(Entity owner, LivingEntity target) {
        if (!(owner instanceof Player) || !(target instanceof Player)) {
            return false;
        }
        return owner.getUUID().equals(target.getUUID()) || DealtTeamManager.areTeammates(owner, target);
    }

    public static boolean shouldSkipFriendlyExceptSelf(Entity owner, LivingEntity target) {
        if (!(owner instanceof Player) || !(target instanceof Player)) {
            return false;
        }
        if (owner.getUUID().equals(target.getUUID())) {
            return false;
        }
        return DealtTeamManager.areTeammates(owner, target);
    }

    private static boolean hasUntargetableGameMode(ServerPlayer player) {
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        return gameMode == GameType.CREATIVE
                || gameMode == GameType.ADVENTURE
                || gameMode == GameType.SPECTATOR;
    }
}
