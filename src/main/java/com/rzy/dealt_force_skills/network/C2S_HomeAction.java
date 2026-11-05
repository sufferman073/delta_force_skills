package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.home.HomeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record C2S_HomeAction(int action, UUID target, int amount) implements CustomPacketPayload {
    public static final Type<C2S_HomeAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "c2s_homeaction"));
    public static final StreamCodec<FriendlyByteBuf, C2S_HomeAction> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_HomeAction::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(C2S_HomeAction msg, FriendlyByteBuf buf) { buf.writeByte(msg.action); buf.writeUUID(msg.target); buf.writeVarInt(msg.amount); }
    public static C2S_HomeAction decode(FriendlyByteBuf buf) { return new C2S_HomeAction(buf.readUnsignedByte(), buf.readUUID(), buf.readVarInt()); }
    public static void handle(C2S_HomeAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> { var player = ctx.player() instanceof net.minecraft.server.level.ServerPlayer p ? p : null; if (player != null) HomeManager.handle(player, msg.action, msg.target, msg.amount); });
    }
}
