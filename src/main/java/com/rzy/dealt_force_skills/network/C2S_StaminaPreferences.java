package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.stamina.StaminaMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public record C2S_StaminaPreferences(int mask) implements CustomPacketPayload {
    public static final Type<C2S_StaminaPreferences> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "stamina_preferences"));
    public static final StreamCodec<FriendlyByteBuf, C2S_StaminaPreferences> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> encode(msg, buf), C2S_StaminaPreferences::decode);
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(C2S_StaminaPreferences msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.mask()); }
    public static C2S_StaminaPreferences decode(FriendlyByteBuf buf) { return new C2S_StaminaPreferences(buf.readVarInt()); }
    public static void handle(C2S_StaminaPreferences msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.stamina.StaminaFeature.apply((net.minecraft.server.level.ServerPlayer) ctx.player(), msg.mask()));
    }
}
