package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.team.DealtTeamEvents;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_TeamSpectatorCycle implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_TeamSpectatorCycle> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_team_spectator_cycle"));

    public static final StreamCodec<FriendlyByteBuf, C2S_TeamSpectatorCycle> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_TeamSpectatorCycle::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int direction;

    public C2S_TeamSpectatorCycle(int direction) {
        this.direction = Integer.compare(direction, 0);
    }

    public static void encode(C2S_TeamSpectatorCycle msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.direction);
    }

    public static C2S_TeamSpectatorCycle decode(FriendlyByteBuf buf) {
        return new C2S_TeamSpectatorCycle(buf.readVarInt());
    }

    public static void handle(C2S_TeamSpectatorCycle msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && msg.direction != 0) {
                DealtTeamManager.cycleSpectatorTarget(player, msg.direction)
                        .ifPresent(DealtTeamEvents::applySpectatorTarget);
            }
        });
    }
}
