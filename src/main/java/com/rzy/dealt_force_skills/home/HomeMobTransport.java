package com.rzy.dealt_force_skills.home;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Selection and movement are entirely server-authoritative; packets never nominate an entity. */
public final class HomeMobTransport {
    private static final Set<Entity> MOVING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private HomeMobTransport() { }
    public static boolean authorized(Entity entity) { return MOVING.contains(entity); }
    private static boolean eligible(LivingEntity mob, ServerPlayer player) {
        if (!mob.isAlive() || mob.isRemoved() || mob.isPassenger() || mob.isVehicle() || !mob.canChangeDimensions()) return false;
        if (!HomeSettings.flag("allow_boss_transport") && (mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss)) return false;
        if (mob instanceof TamableAnimal pet && pet.isTame() && !player.getUUID().equals(pet.getOwnerUUID())) return false;
        return !(mob instanceof AbstractHorse horse) || !horse.isTamed() || player.getUUID().equals(horse.getOwnerUUID());
    }
    public static List<LivingEntity> select(ServerPlayer player) {
        if (!HomeSettings.enabled() || !HomeSettings.flag("allow_mob_transport")) return List.of();
        var preferences = HomeData.get(player.server).transport(player.getUUID());
        List<LivingEntity> selected = new ArrayList<>();
        if (preferences.targeted()) {
            Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(HomeSettings.number("target_range")));
            end = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
            LivingEntity nearest = null; double best = eye.distanceToSqr(end);
            for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1), m -> m != player && !(m instanceof net.minecraft.world.entity.player.Player) && eligible(m, player))) {
                AABB bounds = mob.getBoundingBox().inflate(mob.getPickRadius());
                var hit = bounds.contains(eye) ? java.util.Optional.of(eye) : bounds.clip(eye, end);
                if (hit.isPresent() && eye.distanceToSqr(hit.get()) < best) { best = eye.distanceToSqr(hit.get()); nearest = mob; }
            }
            if (nearest != null) selected.add(nearest);
        }
        if (preferences.nearby()) {
            Vec3 at = player.position();
            var nearby = player.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(at.x - 2, at.y - 2, at.z - 2, at.x + 2, at.y + 2, at.z + 2),
                    mob -> !(mob instanceof net.minecraft.world.entity.player.Player) && eligible(mob, player) && Math.abs(mob.getY() - at.y) <= 2
                            && square(mob.getX() - at.x) + square(mob.getZ() - at.z) <= 4);
            nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
            for (LivingEntity mob : nearby) if (!selected.contains(mob)) selected.add(mob);
        }
        return List.copyOf(selected.subList(0, Math.min(selected.size(), HomeSettings.number("max_carried_mobs"))));
    }
    private static double square(double value) { return value * value; }
    public static int carry(ServerPlayer player, List<? extends LivingEntity> selected, ServerLevel origin, ServerLevel destination, BlockPos around) {
        int count = 0;
        for (LivingEntity mob : selected) {
            if (mob instanceof net.minecraft.world.entity.player.Player || mob.level() != origin || !eligible(mob, player)) continue;
            Vec3 landing = landing(mob, destination, around);
            if (landing == null) continue; // Leave the original untouched when no destination fits.
            Entity moved = null;
            MOVING.add(mob);
            try { moved = move(mob, destination, landing); }
            catch (RuntimeException error) { com.mojang.logging.LogUtils.getLogger().warn("Home transport failed for {}", mob.getUUID(), error); }
            finally { MOVING.remove(mob); }
            if (moved != null && moved.level() == destination && !moved.isRemoved()) {
                moved.setDeltaMovement(Vec3.ZERO); moved.fallDistance = 0;
                if (moved instanceof Mob living) { living.getNavigation().stop(); living.setTarget(null); }
                count++;
            }
        }
        return count;
    }
    private static Vec3 landing(LivingEntity mob, ServerLevel level, BlockPos around) {
        for (int r = 1; r <= 6; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            int x = around.getX() + dx, z = around.getZ() + dz;
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Vec3 pos = new Vec3(x + .5, y, z + .5);
            AABB box = mob.getBoundingBox().move(pos.subtract(mob.position()));
            if (y <= level.getMinBuildHeight() || box.maxY >= level.getMaxBuildHeight()
                    || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(mob, box) || level.containsAnyLiquid(box)) continue;
            boolean supported = true;
            for (int fx = (int) Math.floor(box.minX); fx <= (int) Math.floor(box.maxX - 1e-6); fx++) {
                for (int fz = (int) Math.floor(box.minZ); fz <= (int) Math.floor(box.maxZ - 1e-6); fz++) {
                    BlockPos floor = new BlockPos(fx, y - 1, fz); var state = level.getBlockState(floor);
                    if (!state.isSolidRender(level, floor) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)) supported = false;
                }
            }
            if (supported && level.getBlockStates(box).noneMatch(state -> state.is(net.minecraft.tags.BlockTags.FIRE)
                    || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW))) return pos;
        }
        return null;
    }
    private static Entity move(LivingEntity mob, ServerLevel level, Vec3 pos) {
        return mob.changeDimension(level, new net.minecraftforge.common.util.ITeleporter() {
            @Override public net.minecraft.world.level.portal.PortalInfo getPortalInfo(Entity entity, ServerLevel target,
                    java.util.function.Function<ServerLevel, net.minecraft.world.level.portal.PortalInfo> fallback) {
                return new net.minecraft.world.level.portal.PortalInfo(pos, Vec3.ZERO, mob.getYRot(), mob.getXRot());
            }
            @Override public Entity placeEntity(Entity entity, ServerLevel current, ServerLevel target, float rotation,
                    java.util.function.Function<Boolean, Entity> reposition) {
                return reposition.apply(false);
            }
        });
    }
}
