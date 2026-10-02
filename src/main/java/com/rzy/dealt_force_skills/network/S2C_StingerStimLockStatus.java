package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_StingerStimLockStatus implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_StingerStimLockStatus> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_stinger_stim_lock_status"));

    public static final StreamCodec<FriendlyByteBuf, S2C_StingerStimLockStatus> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_StingerStimLockStatus::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean targetWarning;
    private final int modeOrdinal;
    private final int targetCount;
    private final int durationTicks;

    public S2C_StingerStimLockStatus(boolean targetWarning, int modeOrdinal, int targetCount, int durationTicks) {
        this.targetWarning = targetWarning;
        this.modeOrdinal = modeOrdinal;
        this.targetCount = targetCount;
        this.durationTicks = durationTicks;
    }

    public static void encode(S2C_StingerStimLockStatus msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.targetWarning);
        buf.writeVarInt(msg.modeOrdinal);
        buf.writeVarInt(msg.targetCount);
        buf.writeVarInt(msg.durationTicks);
    }

    public static S2C_StingerStimLockStatus decode(FriendlyByteBuf buf) {
        return new S2C_StingerStimLockStatus(
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_StingerStimLockStatus msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
                    if (msg.targetWarning) {
                        ClientStingerStimLockState.warn(msg.modeOrdinal, msg.durationTicks);
                    } else {
                        ClientStingerHudState.syncStimLockTargets(msg.targetCount, msg.durationTicks);
                    }
                });
    }
}
