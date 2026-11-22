package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.core.BlockPos;
import com.rzy.dealt_force_skills.climb.HvkClimbGlueManager;
import com.rzy.dealt_force_skills.climb.HvkGlueEvents;
import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RaidCables {
    private RaidCables() {}
    private record Ride(java.util.UUID instance, Vec3 bend, Vec3 end, boolean passedBend, double speed) {}
    private static final java.util.Map<ServerPlayer,Ride> rides = new java.util.WeakHashMap<>();
    static void tick(ServerPlayer player, java.util.UUID instance) {
        Ride ride = rides.get(player);
        if (ride == null) return;
        if (!ride.instance.equals(instance) || player.isShiftKeyDown() || !player.isAlive()) { rides.remove(player); return; }
        Vec3 goal = ride.passedBend ? ride.end : ride.bend;
        Vec3 next = advance(player, goal, ride.speed);
        if (next == null) {
            rides.remove(player); player.displayClientMessage(Component.literal("滑索线路受阻，已脱离"),true); return;
        }
        player.connection.teleport(next.x,next.y,next.z,player.getYRot(),player.getXRot());
        player.fallDistance = 0; player.setDeltaMovement(Vec3.ZERO);
        boolean arrived = next.distanceToSqr(goal) < 1.0e-8;
        if (arrived && ride.passedBend) rides.remove(player);
        else if (arrived) rides.put(player,new Ride(instance,ride.bend,ride.end,true,ride.speed));
    }
    static Vec3 advance(ServerPlayer player, Vec3 goal, double speed) {
        Vec3 origin = player.position(), delta = goal.subtract(origin), direction = delta.normalize();
        double length = delta.length();
        for (double distance = Math.min(speed, length); distance <= speed + 6; distance = Math.min(length, distance + .25)) {
            Vec3 candidate = distance >= length ? goal : origin.add(direction.scale(distance));
            var box = player.getBoundingBox().move(candidate.subtract(origin));
            if (player.serverLevel().noCollision(player, box)) return candidate;
            if (distance >= length) return null;
            for (BlockPos pos : BlockPos.betweenClosedStream(box.deflate(.001)).toList()) {
                var state = player.serverLevel().getBlockState(pos);
                if (state.getCollisionShape(player.serverLevel(),pos).isEmpty()) continue;
                var block = state.getBlock();
                if (block != net.minecraft.world.level.block.Blocks.ANDESITE_WALL && block != net.minecraft.world.level.block.Blocks.POLISHED_ANDESITE
                    && block != net.minecraft.world.level.block.Blocks.GRAY_CONCRETE && block != net.minecraft.world.level.block.Blocks.LIGHT_GRAY_STAINED_GLASS_PANE
                    && block != net.minecraft.world.level.block.Blocks.CHAIN) return null;
            }
        }
        return null;
    }
    static void detach(ServerPlayer player) { rides.remove(player); }
    static void restore(ServerLevel level, RaidInstanceData.Instance instance, boolean removing) {
        boolean changed = false;
        for (var cable : RaidTemplate.load(instance.mapId).cables) if (cable.glued()) {
            BlockPos first = BlockPos.containing(cable.first().at(instance.origin()));
            BlockPos second = BlockPos.containing(cable.second().at(instance.origin()));
            if (removing) {
                if (second.equals(HvkClimbGlueManager.partner(level, first))) changed |= HvkClimbGlueManager.unglue(level, first);
            } else {
                level.getChunkAt(first); level.getChunkAt(second);
                if (!HvkClimbGlueManager.isLinked(level, first, second)) {
                    if (!HvkClimbGlueManager.connect(level, first, second, cable.blockSkills()))
                        throw new IllegalStateException("Invalid raid zipline: " + first + " / " + second);
                    changed = true;
                }
            }
        }
        if (changed) HvkGlueEvents.syncLevel(level);
    }
    private static Vec3 landing(LivingEntity actor, RaidInstanceData.Instance instance, Vec3 exit) {
        ServerLevel level=(ServerLevel)actor.level();
        for (int dy : new int[]{0,1,-1,2,-2}) for (int[] offset : new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1}}) {
            Vec3 candidate=exit.add(offset[0],dy,offset[1]); BlockPos pos=BlockPos.containing(candidate);
            if (instance.contains(candidate,0) && level.hasChunkAt(pos) && !level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).isEmpty()
                    && level.noCollision(actor,actor.getBoundingBox().move(candidate.subtract(actor.position())))) return candidate;
        }
        return null;
    }
    public static boolean use(ServerPlayer player, BlockPos hit) {
        var instance=RaidInstances.instanceAt(player.serverLevel(),player.position());
        if (instance==null || !RaidMatches.participating(player)) return false;
        return board(player, hit, instance);
    }
    static boolean board(ServerPlayer player, BlockPos hit, RaidInstanceData.Instance instance) {
        ServerLevel level = player.serverLevel();
        if (HvkClimbGlueManager.isGlued(level, hit)) return false;
        RaidTemplate template = RaidTemplate.load(instance.mapId);
        for (var cable : template.cables) for (boolean reverse : new boolean[]{false,true}) {
            if (cable.glued()) continue;
            var entrance = reverse ? cable.second() : cable.first();
            Vec3 start = entrance.at(instance.origin());
            if (player.distanceToSqr(start)>9 || Vec3.atCenterOf(hit).distanceToSqr(start)>9) continue;
            var anchor = template.boardingAnchor(entrance);
            if (anchor != null) {
                HvkZiplineManager.beginRide(player, level, BlockPos.containing(anchor.at(instance.origin())));
                return true;
            }
            Vec3 exit=landing(player,instance,(reverse?cable.first():cable.second()).at(instance.origin()));
            if (exit!=null) {
                if (rides.containsKey(player)) return true;
                boolean vertical = Math.abs(exit.y-start.y) > Math.sqrt(start.subtract(exit).horizontalDistanceSqr());
                Vec3 low = start.y < exit.y ? start : exit;
                Vec3 bend = new Vec3(low.x,Math.max(start.y,exit.y),low.z);
                rides.put(player,new Ride(instance.id,bend,exit,!vertical,vertical ? .4 : 1.0));
                player.displayClientMessage(Component.literal("滑索 · 潜行脱离"),true);
            }
            else player.displayClientMessage(Component.literal("滑索落点受阻"),true);
            return true;
        }
        return false;
    }
    public static Vec3 route(OperativeEntity actor, Vec3 goal) {
        var instance=RaidInstances.instanceAt(actor.serverLevel(),actor.position());
        if (instance==null || !instance.contains(goal,0)) return goal;
        double best=actor.position().distanceTo(goal); Vec3 waypoint=goal, destination=null;
        for (var cable : RaidTemplate.load(instance.mapId).cables) for (boolean reverse : new boolean[]{false,true}) {
            Vec3 entry=(reverse?cable.second():cable.first()).at(instance.origin()), exit=(reverse?cable.first():cable.second()).at(instance.origin());
            double score=actor.position().distanceTo(entry)+exit.distanceTo(goal)+2;
            if (score<best) { best=score; waypoint=entry; destination=exit; }
        }
        if (destination!=null && actor.distanceToSqr(waypoint)<=9) {
            Vec3 safe=landing(actor,instance,destination);
            if (safe!=null) { actor.getNavigation().stop(); actor.teleportTo(safe.x,safe.y,safe.z); actor.fallDistance=0; }
            return goal;
        }
        return waypoint;
    }
}
