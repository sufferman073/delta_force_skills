package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

/** Client-only camera/lock events; the server validates every observation. */
public record C2S_SkillObservation(int observer, int target) implements CustomPacketPayload {
    public static final Type<C2S_SkillObservation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "c2s_skillobservation"));
    public static final StreamCodec<FriendlyByteBuf, C2S_SkillObservation> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_SkillObservation::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(C2S_SkillObservation msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.observer); buf.writeVarInt(msg.target);
    }
    public static C2S_SkillObservation decode(FriendlyByteBuf buf) {
        return new C2S_SkillObservation(buf.readVarInt(), buf.readVarInt());
    }
    public static void handle(C2S_SkillObservation msg, IPayloadContext ctx) {
        
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.feedback.SkillFeedback.observe(ctx.player() instanceof net.minecraft.server.level.ServerPlayer player ? player : null, msg.observer, msg.target));
        
    }
}
