package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_NTwoFrozenVisualState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_NTwoFrozenVisualState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_n_two_frozen_visual_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_NTwoFrozenVisualState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_NTwoFrozenVisualState::decode
            );

    private final int entityId;
    private final int remainingTicks;

    public S2C_NTwoFrozenVisualState(int entityId, int remainingTicks) {
        this.entityId = entityId;
        this.remainingTicks = Math.max(0, remainingTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_NTwoFrozenVisualState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.remainingTicks);
    }

    public static S2C_NTwoFrozenVisualState decode(FriendlyByteBuf buf) {
        return new S2C_NTwoFrozenVisualState(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(S2C_NTwoFrozenVisualState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> NTwoFrozenVisuals.syncFrozenVisual(msg.entityId, msg.remainingTicks));
    }
}
