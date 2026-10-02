package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public record S2C_RoverState(CompoundTag state) implements CustomPacketPayload {
    public static final Type<S2C_RoverState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "s2c_roverstate"));
    public static final StreamCodec<FriendlyByteBuf, S2C_RoverState> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_RoverState::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_RoverState message, FriendlyByteBuf buffer) { buffer.writeNbt(message.state); }
    public static S2C_RoverState decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt(); return new S2C_RoverState(tag == null ? new CompoundTag() : tag);
    }
    public static void handle(S2C_RoverState message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.RoverClient", "sync", message.state));
    }
}
