package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.util.HearingRangeHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = PlayerList.class, remap = false)
public abstract class HearingSoundBroadcastMixin {
    @Shadow public abstract List<ServerPlayer> getPlayers();

    @Inject(method = "broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V", at = @At("TAIL"))
    private void dealt_force_skills$extendedHearing(Player except, double x, double y, double z,
                                                    double radius, ResourceKey<Level> dimension,
                                                    Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ClientboundSoundPacket) && !(packet instanceof ClientboundSoundEntityPacket)) {
            return;
        }
        // Vanilla already delivered inside its radius; send only the added hearing band.
        for (ServerPlayer listener : getPlayers()) {
            if (listener == except || listener.level().dimension() != dimension) {
                continue;
            }
            double multiplier = HearingRangeHelper.multiplier(listener);
            double distanceSqr = listener.distanceToSqr(x, y, z);
            double extendedRadius = radius * multiplier;
            if (multiplier > 1.0D && distanceSqr >= radius * radius
                    && distanceSqr < extendedRadius * extendedRadius) {
                listener.connection.send(packet);
            }
        }
    }
}
