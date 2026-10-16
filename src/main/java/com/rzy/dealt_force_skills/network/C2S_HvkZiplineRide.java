package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.climb.HvkZiplineManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record C2S_HvkZiplineRide(BlockPos origin) implements CustomPacketPayload {
    public static final Type<C2S_HvkZiplineRide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "hvk_zipline_ride"));
    public static final StreamCodec<FriendlyByteBuf, C2S_HvkZiplineRide> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> encode(msg, buf), C2S_HvkZiplineRide::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void encode(C2S_HvkZiplineRide msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.origin);
    }
    public static C2S_HvkZiplineRide decode(FriendlyByteBuf buf) {
        return new C2S_HvkZiplineRide(buf.readBlockPos());
    }
    public static void handle(C2S_HvkZiplineRide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkZiplineManager.requestRide((net.minecraft.server.level.ServerPlayer) ctx.player(), msg.origin));
    }
}
