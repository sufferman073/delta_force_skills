package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record S2C_HomeDimension(UUID owner) implements CustomPacketPayload {
    public static final Type<S2C_HomeDimension> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "s2c_homedimension"));
    public static final StreamCodec<FriendlyByteBuf, S2C_HomeDimension> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_HomeDimension::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_HomeDimension msg, FriendlyByteBuf buf) { buf.writeUUID(msg.owner); }
    public static S2C_HomeDimension decode(FriendlyByteBuf buf) { return new S2C_HomeDimension(buf.readUUID()); }
    public static void handle(S2C_HomeDimension msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.client.screen.HomeScreen.addDimension(msg.owner));
    }
}
