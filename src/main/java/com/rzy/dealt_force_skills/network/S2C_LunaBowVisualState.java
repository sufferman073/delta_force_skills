package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_LunaBowVisualState(
        int entityId,
        int phase,
        int toolOrdinal,
        int remainingTicks
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_LunaBowVisualState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_luna_bow_visual_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_LunaBowVisualState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_LunaBowVisualState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_LunaBowVisualState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.phase);
        buf.writeVarInt(msg.toolOrdinal);
        buf.writeVarInt(msg.remainingTicks);
    }

    public static S2C_LunaBowVisualState decode(FriendlyByteBuf buf) {
        return new S2C_LunaBowVisualState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_LunaBowVisualState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientLunaBowVisualState.sync(
                        msg.entityId,
                        msg.phase,
                        msg.toolOrdinal,
                        msg.remainingTicks
                ));
    }
}
