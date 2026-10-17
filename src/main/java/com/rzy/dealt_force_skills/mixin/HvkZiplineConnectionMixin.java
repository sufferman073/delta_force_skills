package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla kicks players that look like they float for more than 80 ticks ({@code multiplayer.disconnect.flying}).
 * A rider hanging in mid-air on a long zipline matches that heuristic - no blocks below them, the client
 * reports no downward motion - so the flag is cleared while a traversal is active.
 */
@Mixin(value = ServerGamePacketListenerImpl.class, remap = false)
public abstract class HvkZiplineConnectionMixin {
    @Shadow
    private static boolean containsInvalidValues(double x, double y, double z, float yaw, float pitch) {
        throw new AssertionError();
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void dfs$keepAuthoritativeRidePosition(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl) (Object) this;
        ServerPlayer player = ((HvkZiplineConnectionAccess) this).dfs$player();
        // HEAD can run on Netty. Schedule first, before consulting or mutating ride state.
        PacketUtils.ensureRunningOnSameThread(packet, listener, player.serverLevel());
        if (!HvkZiplineManager.isRiding(player)
                || containsInvalidValues(packet.getX(0), packet.getY(0), packet.getZ(0),
                packet.getYRot(0), packet.getXRot(0))) return;
        player.setYRot(Mth.wrapDegrees(packet.getYRot(player.getYRot())));
        player.setXRot(Mth.wrapDegrees(packet.getXRot(player.getXRot())));
        listener.resetPosition();
        ((HvkZiplineConnectionAccess) this).dfs$setClientIsFloating(false);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void dfs$skipFloatingKickWhileRiding(CallbackInfo ci) {
        HvkZiplineConnectionAccess access = (HvkZiplineConnectionAccess) (Object) this;
        ServerPlayer player = access.dfs$player();
        if (player != null && HvkZiplineManager.isRiding(player)) {
            access.dfs$setClientIsFloating(false);
        }
    }
}
