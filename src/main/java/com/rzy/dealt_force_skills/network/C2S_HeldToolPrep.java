package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.skill.HeldToolVisualSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_HeldToolPrep(int kind) implements CustomPacketPayload {
    public static final Type<C2S_HeldToolPrep> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "held_tool_prep"));
    public static final StreamCodec<FriendlyByteBuf, C2S_HeldToolPrep> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeByte(message.kind), buffer -> new C2S_HeldToolPrep(buffer.readUnsignedByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2S_HeldToolPrep message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) HeldToolVisualSync.queueInput(player, message.kind);
        });
    }
}
