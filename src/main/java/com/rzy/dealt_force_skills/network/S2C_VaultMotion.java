package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public record S2C_VaultMotion(double x, double y, double z, double endX, double endY, double endZ) implements CustomPacketPayload {
    public static final Type<S2C_VaultMotion> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "vault_motion"));
    public static final StreamCodec<FriendlyByteBuf, S2C_VaultMotion> STREAM_CODEC = StreamCodec.of(S2C_VaultMotion::encodeBuffer, S2C_VaultMotion::decode);
    private static void encodeBuffer(FriendlyByteBuf buf, S2C_VaultMotion msg) { encode(msg, buf); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_VaultMotion msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x); buf.writeDouble(msg.y); buf.writeDouble(msg.z);
        buf.writeDouble(msg.endX); buf.writeDouble(msg.endY); buf.writeDouble(msg.endZ);
    }
    public static S2C_VaultMotion decode(FriendlyByteBuf buf) {
        return new S2C_VaultMotion(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    public static void handle(S2C_VaultMotion msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.VaultCameraMotion", "update",
                msg.x, msg.y, msg.z, msg.endX, msg.endY, msg.endZ));
    }
}
