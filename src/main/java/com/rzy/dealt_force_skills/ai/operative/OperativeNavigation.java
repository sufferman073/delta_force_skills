package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

/** Walking and water paths with explicit ladder/vine edges; ordinary walls remain impassable. */
public final class OperativeNavigation extends AmphibiousPathNavigation {
    public OperativeNavigation(Mob mob, Level level) { super(mob, level); }
    @Override protected PathFinder createPathFinder(int nodes) {
        nodeEvaluator = new ClimbingEvaluator();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, nodes);
    }
    private static final class ClimbingEvaluator extends AmphibiousNodeEvaluator {
        ClimbingEvaluator() { super(false); }
        @Override public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
            return level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.CLIMBABLE)
                    ? BlockPathTypes.WALKABLE : super.getBlockPathType(level, x, y, z);
        }
        @Override public int getNeighbors(Node[] neighbors, Node from) {
            int count = super.getNeighbors(neighbors, from);
            BlockPos pos = from.asBlockPos();
            for (int dy : new int[]{-1, 1}) {
                BlockPos to = pos.above(dy);
                if (!level.getBlockState(pos).is(BlockTags.CLIMBABLE)
                        && !level.getBlockState(to).is(BlockTags.CLIMBABLE)) continue;
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
