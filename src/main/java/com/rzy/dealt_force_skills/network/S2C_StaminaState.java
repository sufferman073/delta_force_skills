package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.stamina.StaminaManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public record S2C_StaminaState(double value, double maximum) implements CustomPacketPayload {
    public static final Type<S2C_StaminaState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "stamina_state"));
    public static final StreamCodec<FriendlyByteBuf, S2C_StaminaState> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> encode(msg, buf), S2C_StaminaState::decode);
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(S2C_StaminaState msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.value);
        buf.writeDouble(msg.maximum);
    }

    public static S2C_StaminaState decode(FriendlyByteBuf buf) {
        return new S2C_StaminaState(buf.readDouble(), buf.readDouble());
    }

    public static void handle(S2C_StaminaState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.StaminaHud", "update", msg.value, msg.maximum));
    }
}
