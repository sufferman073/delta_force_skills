package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.gambler.GamblerStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class C2S_GamblerDuelInvite implements CustomPacketPayload {
    private static final int MAX_INVITES = 64;
    public static final Type<C2S_GamblerDuelInvite> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_gambler_duel_invite"));
    public static final StreamCodec<FriendlyByteBuf, C2S_GamblerDuelInvite> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_GamblerDuelInvite::decode);

    private final List<UUID> invitedIds;

    public C2S_GamblerDuelInvite(List<UUID> invitedIds) {
        this.invitedIds = invitedIds == null ? List.of() : List.copyOf(invitedIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_GamblerDuelInvite msg, FriendlyByteBuf buf) {
        int count = Math.min(MAX_INVITES, msg.invitedIds.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeUUID(msg.invitedIds.get(i));
        }
    }

    public static C2S_GamblerDuelInvite decode(FriendlyByteBuf buf) {
        int count = Math.min(MAX_INVITES, Math.max(0, buf.readVarInt()));
        List<UUID> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buf.readUUID());
        }
        return new C2S_GamblerDuelInvite(ids);
    }

    public static void handle(C2S_GamblerDuelInvite msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                GamblerStateManager.confirmFinalBetInvite(player, msg.invitedIds);
            }
        });
    }
}
