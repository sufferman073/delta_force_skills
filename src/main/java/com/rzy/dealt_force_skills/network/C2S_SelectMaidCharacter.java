package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.compat.maid.MaidSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record C2S_SelectMaidCharacter(UUID token, String characterId) implements CustomPacketPayload {
    public static final Type<C2S_SelectMaidCharacter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "c2s_selectmaidcharacter"));
    public static final StreamCodec<FriendlyByteBuf, C2S_SelectMaidCharacter> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_SelectMaidCharacter::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(C2S_SelectMaidCharacter msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.token); buf.writeUtf(msg.characterId, 256);
    }
    public static C2S_SelectMaidCharacter decode(FriendlyByteBuf buf) {
        return new C2S_SelectMaidCharacter(buf.readUUID(), buf.readUtf(256));
    }
    public static void handle(C2S_SelectMaidCharacter msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) MaidSelection.submit(serverPlayer, msg.token, msg.characterId);
        });
    }
}
