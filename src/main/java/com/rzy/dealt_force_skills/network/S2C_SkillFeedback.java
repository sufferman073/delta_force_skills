package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-authored visual feedback. Never accepted from a client. */
public record S2C_SkillFeedback(CompoundTag data) implements CustomPacketPayload {
    public static final Type<S2C_SkillFeedback> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "s2c_skillfeedback"));
    public static final StreamCodec<FriendlyByteBuf, S2C_SkillFeedback> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_SkillFeedback::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_SkillFeedback packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.data); }
    public static S2C_SkillFeedback decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new S2C_SkillFeedback(tag == null ? new CompoundTag() : tag);
    }
    public static void handle(S2C_SkillFeedback packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.client.ClientSkillFeedback.accept(packet.data));
    }
}
