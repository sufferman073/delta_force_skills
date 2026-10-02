package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.config.ConfigAccess;
import com.rzy.dealt_force_skills.config.ConfigEntryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class C2S_RequestConfigSnapshot implements CustomPacketPayload {
    public static final Type<C2S_RequestConfigSnapshot> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_request_config_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, C2S_RequestConfigSnapshot> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_RequestConfigSnapshot::decode);

    private static final int CHUNK_SIZE = 80;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(C2S_RequestConfigSnapshot msg, FriendlyByteBuf buf) {
    }

    public static C2S_RequestConfigSnapshot decode(FriendlyByteBuf buf) {
        return new C2S_RequestConfigSnapshot();
    }

    public static void handle(C2S_RequestConfigSnapshot msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("config.dealt_force_skills.error.no_permission"));
                NetworkHandler.sendToPlayer(new S2C_ConfigSnapshot(0, 0, false, true, List.of()), player);
                return;
            }
            sendSnapshot(player, true);
        });
    }

    public static void sendSnapshot(ServerPlayer player, boolean canEdit) {
            List<ConfigEntryData> all = ConfigAccess.snapshotAllServerFiles();
            if (all.isEmpty()) {
                NetworkHandler.sendToPlayer(new S2C_ConfigSnapshot(0, 1, canEdit, true, List.of()), player);
                return;
            }
            int totalChunks = (all.size() + CHUNK_SIZE - 1) / CHUNK_SIZE;
            for (int chunk = 0; chunk < totalChunks; chunk++) {
                int from = chunk * CHUNK_SIZE;
                int to = Math.min(all.size(), from + CHUNK_SIZE);
                List<ConfigEntryData> slice = new ArrayList<>(all.subList(from, to));
                NetworkHandler.sendToPlayer(
                        new S2C_ConfigSnapshot(chunk, totalChunks, canEdit, chunk == totalChunks - 1, slice),
                        player);
            }
    }
}
