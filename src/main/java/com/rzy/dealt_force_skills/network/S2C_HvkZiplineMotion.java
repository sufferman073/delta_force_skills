package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-driven zipline position sample.
 *
 * <p>The traversal is authoritative on the server; this packet mirrors the player's own position on the
 * riding client so the ride is smooth without a carrier entity. A plain position sample (rather than a
 * {@code ClientboundPlayerPositionPacket}) keeps the connection out of {@code awaitingPositionFromClient},
 * so look rotation and local packets are never dropped while riding.</p>
 */
public record S2C_HvkZiplineMotion(boolean active, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<S2C_HvkZiplineMotion> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "hvk_zipline_motion"));
    public static final StreamCodec<FriendlyByteBuf, S2C_HvkZiplineMotion> STREAM_CODEC = StreamCodec.of(S2C_HvkZiplineMotion::encodeBuffer, S2C_HvkZiplineMotion::decode);
    private static void encodeBuffer(FriendlyByteBuf buf, S2C_HvkZiplineMotion msg) { encode(msg, buf); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_HvkZiplineMotion msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeDouble(msg.x); buf.writeDouble(msg.y); buf.writeDouble(msg.z);
    }
    public static S2C_HvkZiplineMotion decode(FriendlyByteBuf buf) {
        return new S2C_HvkZiplineMotion(buf.readBoolean(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    public static void handle(S2C_HvkZiplineMotion msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.HvkZiplineMotionClient", "apply",
                msg.active, msg.x, msg.y, msg.z));
    }
}
