package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.stamina.StaminaMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public record C2S_StaminaVault() implements CustomPacketPayload {
    public static final Type<C2S_StaminaVault> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "stamina_vault"));
    public static final StreamCodec<FriendlyByteBuf, C2S_StaminaVault> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> encode(msg, buf), C2S_StaminaVault::decode);
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(C2S_StaminaVault msg, FriendlyByteBuf buf) { }
    public static C2S_StaminaVault decode(FriendlyByteBuf buf) { return new C2S_StaminaVault(); }
    public static void handle(C2S_StaminaVault msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> StaminaMovement.tryVault((net.minecraft.server.level.ServerPlayer) ctx.player()));
    }
}
