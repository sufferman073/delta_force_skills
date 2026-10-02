package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.music.FoxMusicPlayerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public record C2S_FoxMusicAction(UUID device, FoxMusicPlayerManager.Action action, int index, int page) implements CustomPacketPayload {
    public static final Type<C2S_FoxMusicAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "fox_music_action"));
    public static final StreamCodec<FriendlyByteBuf, C2S_FoxMusicAction> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_FoxMusicAction::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(C2S_FoxMusicAction msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.device); buf.writeEnum(msg.action); buf.writeVarInt(msg.index); buf.writeVarInt(msg.page);
    }
    public static C2S_FoxMusicAction decode(FriendlyByteBuf buf) {
        return new C2S_FoxMusicAction(buf.readUUID(), buf.readEnum(FoxMusicPlayerManager.Action.class), buf.readVarInt(), buf.readVarInt());
    }
    public static void handle(C2S_FoxMusicAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player() instanceof net.minecraft.server.level.ServerPlayer server ? server : null;
            if (player != null) FoxMusicPlayerManager.action(player, msg.device, msg.action, msg.index, msg.page);
        });
    }
}
