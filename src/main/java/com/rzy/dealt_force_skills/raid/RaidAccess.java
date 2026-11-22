package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.raid.ai.RaidAiAffinity;
import com.rzy.dealt_force_skills.raid.ui.RaidAssetMenus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Server authority shared by legacy entry packets and teleport guards. */
public final class RaidAccess {
    private RaidAccess() { }
    public static boolean externalItemsBlocked(ServerPlayer player) {
        return RaidMatches.belongs(player) || RaidMatches.assetsBlocked(player);
    }
    public static boolean baseEntrance(ServerPlayer player, boolean open) {
        if (RaidMatches.assetsBlocked(player)) return true;
        if (!RaidMatches.belongs(player)) return false;
        if (open && RaidMatches.participating(player)) RaidAssetMenus.openStorage(player);
        return true;
    }
    public static boolean canTeleport(Entity entity, ServerLevel target, double x, double y, double z) {
        if (RaidInstances.authorizedTravel(entity)) return true;
        if (!(entity.level() instanceof ServerLevel from)) return true;
        if (entity instanceof ServerPlayer player && RaidMatches.assetsBlocked(player)) return false;
        if (!from.dimension().equals(RaidInstances.DIMENSION) && !target.dimension().equals(RaidInstances.DIMENSION)) return true;
        if (from != target || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        Vec3 point = new Vec3(x, y, z);
        if (entity instanceof ServerPlayer player) {
            var match = RaidMatches.match(player);
            if (match != null) {
                var instance = RaidInstanceData.get(from.getServer()).instance(match.id);
                return instance != null && instance.contains(point, 0) && y > target.getMinBuildHeight() && y < target.getMaxBuildHeight();
            }
        }
        if (RaidAiAffinity.instance(entity) != null) return RaidAiAffinity.inside(entity, point);
        var instance = RaidInstances.instanceAt(from, entity.position());
        return instance != null && instance.contains(point, 0);
    }
}
