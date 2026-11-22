package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

/** Dry ground paths with explicit ladder/vine edges; ordinary walls remain impassable. */
public final class OperativeNavigation extends GroundPathNavigation {
    public OperativeNavigation(Mob mob, Level level) { super(mob, level); }
    @Override protected PathFinder createPathFinder(int nodes) {
        nodeEvaluator = new ClimbingEvaluator();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, nodes);
    }
    @Override public Path createPath(BlockPos target, int accuracy) {
        if (level.getBlockState(target).is(BlockTags.CLIMBABLE)) return createPath(java.util.Set.of(target), accuracy);
        return super.createPath(target, accuracy);
    }
    @Override protected boolean canUpdatePath() {
        return mob.onClimbable() || super.canUpdatePath();
    }
    @Override public void tick() {
        boolean raid = mob.level().dimension().equals(com.rzy.dealt_force_skills.raid.RaidInstances.DIMENSION);
        nodeEvaluator.setCanOpenDoors(raid);
        if (raid && mob.level() instanceof net.minecraft.server.level.ServerLevel server && path != null && !path.isDone()) {
            for (int index=Math.max(0,path.getNextNodeIndex()-1); index<Math.min(path.getNodeCount(),path.getNextNodeIndex()+2);index++) {
                BlockPos pos=path.getNode(index).asBlockPos(); var state=server.getBlockState(pos);
                if (state.is(BlockTags.WOODEN_DOORS) && state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock door
                        && !state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) && mob.distanceToSqr(Vec3.atCenterOf(pos))<6.25
                        && com.rzy.dealt_force_skills.raid.RaidMatches.mayOpenWoodenDoor(server,mob,pos)) door.setOpen(mob,server,state,pos,true);
            }
        }
        super.tick();
    }
    private static final class ClimbingEvaluator extends WalkNodeEvaluator {
        @Override public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
            if (level.getFluidState(new BlockPos(x, y, z)).is(net.minecraft.tags.FluidTags.WATER)) return BlockPathTypes.BLOCKED;
            return level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.CLIMBABLE)
                    ? BlockPathTypes.WALKABLE : super.getBlockPathType(level, x, y, z);
        }
        @Override public int getNeighbors(Node[] neighbors, Node from) {
            int candidates = super.getNeighbors(neighbors, from), count = 0;
            for (int index = 0; index < candidates; index++) {
                BlockPos feet = neighbors[index].asBlockPos();
                if (mob.level().getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER)
                        || mob.level().getFluidState(feet.above()).is(net.minecraft.tags.FluidTags.WATER)) continue;
                neighbors[count++] = neighbors[index];
            }
            BlockPos pos = from.asBlockPos();
            for (int dy : new int[]{-1, 1}) {
                BlockPos to = pos.above(dy);
                if (!level.getBlockState(pos).is(BlockTags.CLIMBABLE)
                        && !level.getBlockState(to).is(BlockTags.CLIMBABLE)) continue;
                if (mob.level().getFluidState(to).is(net.minecraft.tags.FluidTags.WATER)) continue;
                Vec3 point = Vec3.atBottomCenterOf(to);
                if (!level.noCollision(mob, mob.getBoundingBox().move(point.subtract(mob.position())).deflate(.02))) continue;
                Node node = getNode(to.getX(), to.getY(), to.getZ());
                if (node.closed || count >= neighbors.length) continue;
                node.type = BlockPathTypes.WALKABLE; node.costMalus = 0;
                boolean duplicate = false;
                for (int i = 0; i < count; i++) if (neighbors[i] == node) duplicate = true;
                if (!duplicate) neighbors[count++] = node;
            }
            return count;
        }
    }
}
