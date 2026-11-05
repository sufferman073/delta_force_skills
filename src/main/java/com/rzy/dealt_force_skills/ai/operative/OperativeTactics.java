package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.*;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** A small, bounded local planner. Every path is checked against terrain, orders and blast zones. */
public final class OperativeTactics {
    private record Hazard(Vec3 point, double radius) { }
    private final OperativeEntity host;
    private final List<Hazard> hazards = new ArrayList<>();
    private int nextMove, blockedSince = -1, progressTick;
    private Vec3 progressAt, failedPoint;
    private String movement = "";
    public OperativeTactics(OperativeEntity host) { this.host = host; }

    public void perceive() {
        hazards.clear();
        int count = 0;
        for (Entity e : host.level().getEntities(host, host.getBoundingBox().inflate(20), Entity::isAlive)) {
            double radius = radius(e);
            if (radius <= 0) continue;
            Entity caster = e instanceof Projectile p ? p.getOwner() : null;
            if (caster != null && !TargetingUtil.isSelfOrHostileLivingFor(caster, host)) continue;
            hazards.add(new Hazard(e.position(), radius + 1.2));
            if (e instanceof Projectile && e.getDeltaMovement().lengthSqr() > .01) {
                Vec3 end = e.position().add(e.getDeltaMovement().scale(8)).add(0, -1, 0);
                var hit = host.level().clip(new ClipContext(e.position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
                hazards.add(new Hazard(hit.getType() == HitResult.Type.MISS ? end : hit.getLocation(), radius + 1.2));
            }
            if (++count >= 32) break;
        }
    }

    private static double radius(Entity e) {
        if (e instanceof net.minecraft.world.entity.item.PrimedTnt) return 7;
        if (e instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow && !arrow.onGround()) return 1;
        if (e instanceof VyronMagneticBombEntity) return VyronMagneticBombEntity.blastRadius();
        if (e instanceof VyronTigerCannonEntity) return VyronTigerCannonEntity.blastRadius();
        if (e instanceof DWolfHandCannonGrenadeEntity p) return OperativeGrowth.damageRadius(p.getOwner(), DWolfHandCannonGrenadeEntity.blastRadius(), "summons.dwolfhandcannongrenadeentity");
        if (e instanceof LunaCompositeGrenadeEntity p) return OperativeGrowth.damageRadius(p.getOwner(), LunaCompositeGrenadeEntity.blastRadius(), "summons.lunacompositegrenadeentity");
        if (e instanceof ShepherdFragGrenadeEntity) return ShepherdFragGrenadeEntity.nativeRadius();
        if (e instanceof UluruLoiteringMissileEntity p) return UluruLoiteringMissileEntity.nativeRadius(p.getOwner());
        if (e instanceof UluruBombletEntity p) return UluruBombletEntity.nativeRadius(p.getOwner());
        if (e instanceof UluruFireFieldEntity) return UluruFireFieldEntity.nativeRadius();
        if (e instanceof UluruIncendiaryGrenadeEntity || e instanceof SaeedFireFieldEntity) return 6;
        if (e instanceof NTwoCondensedGrenadeEntity || e instanceof NTwoDewarCanisterEntity) return 5;
        if (e instanceof NTwoIceFieldEntity || e instanceof ToxikTearGasCloudEntity) return 6;
        return 0;
    }

    public double danger(Vec3 point) {
        double danger = 0;
        for (Hazard h : hazards) {
            double distance = point.distanceTo(h.point);
            if (distance < h.radius) danger += (h.radius - distance) * 12 + 20;
        }
        return danger;
    }

    public boolean threatened() { return danger(host.position()) > 0; }

    public boolean move(LivingEntity target) {
        if (host.skills().locksMovement()) return false;
        boolean gun = OperativeGun.equipped(host) && host.gun().hasAmmunition(host);
        double distance = target == null ? 0 : host.distanceTo(target);
        boolean blocked = gun && target != null && !OperativeGun.clearShot(host, target);
        if (!blocked) blockedSince = -1;
        else if (blockedSince < 0) blockedSince = host.tickCount;
        double preferred = preferredDistance();
        // A range is useful only while it permits an attack. Close in stages to open an angle.
        if (blocked) preferred = Math.max(host.skills() instanceof NativeManbaSkills ? 3 : 8, Math.min(preferred, distance - 3 - Math.min(24, (host.tickCount - blockedSince) / 10.0)));
        // Elbow approaches must reach its actual range rather than stopping three blocks short.
        double approachMargin = host.skills() instanceof NativeManbaSkills ? .3 : 3;
        boolean approach = target != null && distance > preferred + approachMargin;
        boolean retreat = threatened() || gun && target != null && !blocked
                && (distance < preferred * .7 || host.gun().reloading(host) || host.getHealth() < host.getMaxHealth() * .35);
        boolean strafe = gun && target != null && distance <= preferred * 1.6;
        if (!retreat && !blocked && !approach && !strafe) return false;
        Vec3 origin = host.position();
        boolean stuck = false;
        if (host.tickCount >= progressTick) {
            stuck = progressAt != null && !host.getNavigation().isDone() && origin.distanceToSqr(progressAt) < .25;
            progressAt = origin; progressTick = host.tickCount + 30;
            if (stuck && host.getNavigation().getTargetPos() != null)
                failedPoint = Vec3.atBottomCenterOf(host.getNavigation().getTargetPos());
        }
        double baseline = danger(origin);
        if (!stuck && host.tickCount < nextMove && !host.getNavigation().isDone()
                && safePath(host.getNavigation().getPath(), baseline)) return true;
        nextMove = host.tickCount + 40;
        Vec3 best = null;
        net.minecraft.world.level.pathfinder.Path bestPath = null;
        double bestScore = -Double.MAX_VALUE;
        for (int pass = 0; pass < 2 && best == null; pass++) {
            // The second pass allows a small advance if every retreat lane is blocked.
            for (int ring = 1; ring <= 3; ring++) for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6 + (host.getId() % 4) * .2;
                Vec3 point = ground(origin.add(Math.cos(angle) * ring * 4, 0, Math.sin(angle) * ring * 4));
                if (point == null || !host.withinOrders(point) || danger(point) > baseline) continue;
                double nextDistance = target == null ? 0 : point.distanceTo(target.position());
                if (target != null && (retreat && pass == 0 && nextDistance < distance
                        || gun && nextDistance < Math.min(host.skills() instanceof NativeManbaSkills ? 2.4 : 4, distance) && !threatened())) continue;
                var path = host.getNavigation().createPath(BlockPos.containing(point), 0);
                if (path == null || !path.canReach() || !safePath(path, baseline)) continue;
                double score = -danger(point) * 8 - path.getNodeCount() * .15;
                if (failedPoint != null) score -= Math.max(0, 6 - point.distanceTo(failedPoint)) * 12;
                if (target != null) {
                    score -= Math.abs(nextDistance - preferred) * 2;
                    if (retreat) score += Math.min(nextDistance, preferred + 8);
                    boolean shot = OperativeGun.attackPoint(host, point.add(0, host.getEyeHeight(), 0), target, true) != null;
                    boolean cover = !OperativeGun.unobstructed(host, target.getEyePosition(), point.add(0, host.getBbHeight() * .45, 0));
                    score += shot ? 20 : blocked && nextDistance < distance ? 2 : -18;
                    score += cover ? (retreat ? 24 : shot ? 14 : -4) : 0;
                }
                if (score > bestScore) { best = point; bestPath = path; bestScore = score; }
            }
        }
        if (best == null) {
            if (threatened()) { host.getNavigation().stop(); movement = "hazard_blocked"; return true; }
            // Let the ordinary pathfinder try a longer route around a wall, rather than oscillating.
            if (target != null && (approach || blocked)) {
                var path = host.getNavigation().createPath(target, 0);
                if (path != null && safePath(path, baseline)) {
                    host.getNavigation().moveTo(path, 1.1); movement = "pursue_route"; return true;
                }
            }
            return false;
        }
        host.getNavigation().moveTo(bestPath, retreat ? 1.25 : 1.1);
        movement = threatened() ? "evade_hazard" : retreat ? "retreat" : blocked ? "reposition" : approach ? "pursue" : "skirmish";
        return true;
    }

    public boolean safePath(net.minecraft.world.level.pathfinder.Path path, double baseline) {
        if (path == null) return false;
        for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
            var node = path.getNode(i);
            if (!host.withinOrders(new Vec3(node.x + .5, node.y, node.z + .5))
                    || danger(new Vec3(node.x + .5, node.y, node.z + .5)) > baseline + .1 || unsafe(new BlockPos(node.x, node.y, node.z))) return false;
        }
        return true;
    }

    public boolean navigate(Vec3 destination, double speed) {
        var path = host.getNavigation().createPath(BlockPos.containing(destination), 0);
        if (path == null || !safePath(path, danger(host.position()))) { host.getNavigation().stop(); return false; }
        return host.getNavigation().moveTo(path, speed);
    }

    private Vec3 ground(Vec3 p) {
        BlockPos base = BlockPos.containing(p);
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos at = base.offset(0, dy, 0);
            if (!host.level().hasChunkAt(at) || unsafe(at)) continue;
            Vec3 result = Vec3.atBottomCenterOf(at);
            var box = host.getBoundingBox().move(result.subtract(host.position())).deflate(.02);
            if (host.level().noCollision(host, box) && (!host.level().noCollision(host, box.move(0, -.4, 0)) || host.level().getFluidState(at).is(net.minecraft.tags.FluidTags.WATER))) return result;
        }
        return null;
    }

    private boolean unsafe(BlockPos p) { return unsafe(host, p); }
    private static boolean unsafe(OperativeHost source, BlockPos p) {
        for (int dy = -1; dy <= 1; dy++) {
            var state = source.serverLevel().getBlockState(p.above(dy));
            if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) return true;
        }
        return false;
    }

    public double preferredDistance() {
        if (!OperativeGun.equipped(host)) return 2;
        if (host.skills() instanceof NativeDepartmentSkills department && department.wantsLaser(host)) return 11;
        if (host.skills() instanceof NativeManbaSkills manba) return manba.combatRange();
        return OperativeGun.preferredRange(host);
    }

    /** Choose a complete displacement lane before committing to an unsteerable boost. */
    public static Vec3 displacement(OperativeHost host, LivingEntity target, double travel) {
        if (target == null || travel <= 0 || !Double.isFinite(travel)) return Vec3.ZERO;
        Vec3 origin = host.body().position();
        boolean gun = host.body() instanceof OperativeEntity bot && OperativeGun.equipped(bot)
                && bot.gun().hasAmmunition(bot)
                || host instanceof com.rzy.dealt_force_skills.compat.maid.MaidSkillHost maid && maid.dealtForceMaidSkills().rangedStrategy();
        double preferred = gun ? OperativeGun.preferredRange(host.body()) : 3;
        double current = origin.distanceTo(target.position());
        boolean retreat = gun && current < preferred || host.body().getHealth() < host.body().getMaxHealth() * .5F;
        Vec3 best = Vec3.ZERO;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8;
            Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 end = origin.add(direction.scale(travel));
            double distance = end.distanceTo(target.position());
            if ((gun || retreat) && distance < Math.min(current, retreat ? current : preferred)
                    || distance < 3 || !safeDisplacement(host, direction, travel)) continue;
            // Check the closest point of the entire lane, including a dash past a target.
            if (gun) {
                double along = Math.max(0, Math.min(travel, target.position().subtract(origin).dot(direction)));
                if (origin.add(direction.scale(along)).distanceTo(target.position()) < Math.min(current, preferred) - .1D) continue;
            }
            double score = retreat ? distance : -Math.abs(distance - preferred);
            if (score > bestScore) { best = direction; bestScore = score; }
        }
        return best;
    }

    public static boolean safeDisplacement(OperativeHost host, Vec3 direction, double travel) {
        if (direction.lengthSqr() < .5 || !Double.isFinite(travel) || travel <= 0 || travel > 96) return false;
        OperativeTactics threats = null;
        if (host.body() instanceof OperativeEntity bot) { threats = new OperativeTactics(bot); threats.perceive(); }
        double baseline = threats == null ? 0 : threats.danger(host.body().position());
        for (double d = .5; d <= travel + .49; d += .5) {
            Vec3 delta = direction.scale(Math.min(d, travel)), point = host.body().position().add(delta);
            var box = host.body().getBoundingBox().move(delta).deflate(.02);
            if (!host.withinOrders(point) || !host.serverLevel().hasChunkAt(BlockPos.containing(point))
                    || !host.serverLevel().getWorldBorder().isWithinBounds(box)
                    || !host.serverLevel().noCollision(host.body(), box) || host.serverLevel().containsAnyLiquid(box)
                    || host.serverLevel().noCollision(host.body(), box.move(0, -1, 0))
                    || unsafe(host, BlockPos.containing(point))
                    || threats != null && threats.danger(point) > baseline + .1) return false;
        }
        return true;
    }
    public String describe() { return movement; }
}
