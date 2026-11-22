package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.raid.RaidAccess;
import com.rzy.dealt_force_skills.raid.RaidMatches;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public abstract class RaidConnectionMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), cancellable = true)
    private void dfs$raidDestination(double x, double y, double z, float yaw, float pitch, Set<RelativeMovement> relative, CallbackInfo ci) {
        // The arguments are the absolute target; relative flags only control client packet encoding.
        if (!RaidAccess.canTeleport(player, player.serverLevel(), x, y, z)) ci.cancel();
    }
    @Inject(method = {"handleContainerClick", "handlePlayerAction", "handleUseItem", "handleUseItemOn", "handleSetCreativeModeSlot"}, at = @At("HEAD"), cancellable = true)
    private void dfs$raidPendingAssets(CallbackInfo ci) {
        if (player.server.isSameThread() && RaidMatches.assetsBlocked(player)) ci.cancel();
    }
}
