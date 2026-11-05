package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

/** Reclaim spent deployment locations while retaining guard/ambush lanes and nearby threats. */
public final class OperativeTrapMaintenance {
    private OperativeTrapMaintenance() { }
    public static boolean stale(Entity trap, OperativeAllegiance allegiance, double radius) {
        if (!allegiance.active() || allegiance.caster() == null || trap.isPassenger() || trap.isVehicle() || trap.tickCount < 200 || trap.tickCount % 40 != 0
                || !(trap.level() instanceof ServerLevel level)
                || !(level.getEntity(allegiance.caster()) instanceof OperativeHost root)) return false;
        return stale(level, trap.position(), allegiance, radius);
    }
    public static boolean stale(ServerLevel level, net.minecraft.world.phys.Vec3 position, OperativeAllegiance allegiance, double radius) {
        if (!allegiance.active() || allegiance.caster() == null
                || !(level.getEntity(allegiance.caster()) instanceof OperativeHost root)) return false;
        double watch = Math.max(10, radius + 5);
        var origin = position.add(0,.25,0);
        boolean threat = !level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(position, position).inflate(watch),
                e -> root.isEnemy(e) && e.distanceToSqr(position) <= watch * watch
                        && level.clip(new ClipContext(origin,e.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,root.body())).getType()==HitResult.Type.MISS).isEmpty();
        if (threat) return false;
        boolean sealed = true;
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var end=origin.add(direction.getStepX()*.8,0,direction.getStepZ()*.8);
            if (level.clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,root.body())).getType()==HitResult.Type.MISS) { sealed=false; break; }
        }
        if (sealed) return true;
        if (root instanceof OperativeEntity bot && bot.isGuarding() && position.distanceToSqr(bot.orderPosition())<=16*16) return false;
        if (root.body() instanceof net.minecraft.world.entity.TamableAnimal maid && maid.isInSittingPose()) return false;
        LivingEntity target=root.body() instanceof net.minecraft.world.entity.Mob mob ? mob.getTarget() : null;
        return root.body().distanceToSqr(position)>18*18 || target!=null && root.isEnemy(target)
                && target.distanceToSqr(position)>20*20 && root.body().distanceToSqr(position)>8*8;
    }
}
