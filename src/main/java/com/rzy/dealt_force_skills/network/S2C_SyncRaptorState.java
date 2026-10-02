package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncRaptorState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncRaptorState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_raptor_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncRaptorState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncRaptorState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int falconCooldownTicks;
    private final int pulseCharges;
    private final int pulseMaxCharges;
    private final int pulseRechargeTicks;
    private final int hummingbirdCooldownTicks;
    private final int equippedToolOrdinal;
    private final int falconActiveTicks;
    private final int hummingbirdPendingTicks;
    private final int hummingbirdAttachTicks;

    public S2C_SyncRaptorState(
            int falconCooldownTicks,
            int pulseCharges,
            int pulseMaxCharges,
            int pulseRechargeTicks,
            int hummingbirdCooldownTicks,
            int equippedToolOrdinal,
            int falconActiveTicks,
            int hummingbirdPendingTicks,
            int hummingbirdAttachTicks
    ) {
        this.falconCooldownTicks = falconCooldownTicks;
        this.pulseCharges = pulseCharges;
        this.pulseMaxCharges = pulseMaxCharges;
        this.pulseRechargeTicks = pulseRechargeTicks;
        this.hummingbirdCooldownTicks = hummingbirdCooldownTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
        this.falconActiveTicks = falconActiveTicks;
        this.hummingbirdPendingTicks = hummingbirdPendingTicks;
        this.hummingbirdAttachTicks = hummingbirdAttachTicks;
    }

    public static void encode(S2C_SyncRaptorState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.falconCooldownTicks);
        buf.writeVarInt(msg.pulseCharges);
        buf.writeVarInt(msg.pulseMaxCharges);
        buf.writeVarInt(msg.pulseRechargeTicks);
        buf.writeVarInt(msg.hummingbirdCooldownTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
        buf.writeVarInt(msg.falconActiveTicks);
        buf.writeVarInt(msg.hummingbirdPendingTicks);
        buf.writeVarInt(msg.hummingbirdAttachTicks);
    }

    public static S2C_SyncRaptorState decode(FriendlyByteBuf buf) {
        return new S2C_SyncRaptorState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_SyncRaptorState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientRaptorHudState.sync(
                        msg.falconCooldownTicks,
                        msg.pulseCharges,
                        msg.pulseMaxCharges,
                        msg.pulseRechargeTicks,
                        msg.hummingbirdCooldownTicks,
                        msg.equippedToolOrdinal,
                        msg.falconActiveTicks,
                        msg.hummingbirdPendingTicks,
                        msg.hummingbirdAttachTicks
                ));
    }
}
