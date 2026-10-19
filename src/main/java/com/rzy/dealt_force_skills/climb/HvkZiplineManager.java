package com.rzy.dealt_force_skills.climb;

import com.rzy.dealt_force_skills.entity.HvkZiplineSeatEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;

public final class HvkZiplineManager {
    public static final double BOARDING_RANGE = 5.0;
    private HvkZiplineManager() { }
    public static boolean isRiding(ServerPlayer player) { return player.getVehicle() instanceof HvkZiplineSeatEntity; }
    public static boolean blocksSkills(ServerPlayer player) {
        return player != null && player.getVehicle() instanceof HvkZiplineSeatEntity seat && seat.blocksSkills();
    }
    public static boolean blocksToolAction(ServerPlayer player, Enum<?> action) {
        if (!blocksSkills(player)) return false;
        // Cleanup and release messages still run; this mode does not clear active effects.
        return switch (action.name()) {
            case "STOW", "STOW_TOOL", "STOP_FLASHLIGHT", "CANCEL_DUEL", "CALIBRATION_RELEASE", "RECALL_MARKER" -> false;
            default -> true;
        };
    }
    public static boolean canReach(Player player, BlockPos origin) {
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(origin)) <= BOARDING_RANGE * BOARDING_RANGE;
    }
    /** Exact outline hit, including nearer blocks; do not attract clicks from neighboring blocks. */
    public static BlockPos lookedAtBlock(Player player) {
        Vec3 eye = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(BOARDING_RANGE)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }
    public static boolean isLookingAt(Player player, BlockPos origin) {
        return canReach(player, origin) && origin.equals(lookedAtBlock(player));
    }
    public static void requestRide(ServerPlayer player, BlockPos origin) {
        if (player != null && player.level() instanceof ServerLevel level && isLookingAt(player, origin)) {
            beginRide(player, level, origin);
        }
    }
    public static boolean beginRide(ServerPlayer player, ServerLevel level, BlockPos origin) {
        if (isRiding(player)) return true;
        BlockPos destination = HvkClimbGlueManager.partner(level, origin);
        if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.isSleeping()
                || player.level() != level || !canReach(player, origin)
                || !HvkClimbGlueManager.isLinked(level, origin, destination)) {
            player.displayClientMessage(Component.translatable("message.dealt_force_skills.hvk_universal_glue.zipline_no_path"), true);
            return false;
        }
        HvkZiplineSeatEntity seat = new HvkZiplineSeatEntity(ModEntities.HVK_ZIPLINE_SEAT.get(), level);
        seat.configure(origin, destination, player.position(), HvkClimbGlueManager.blocksSkills(level, origin), player.isShiftKeyDown());
        if (!level.addFreshEntity(seat)) return false;
        if (!player.startRiding(seat, true)) { seat.discard(); return false; }
        player.fallDistance = 0;
        return true;
    }
}
