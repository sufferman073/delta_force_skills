package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncCorpsState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncCorpsState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_corps_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncCorpsState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncCorpsState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int active1CooldownTicks;
    private final int active2CooldownTicks;
    private final int coreCooldownTicks;
    private final int duelRemainingTicks;
    private final int loyaltyOrangeStacks;
    private final boolean duelTargetMarkerActive;
    private final String duelTargetMarkerDimension;
    private final double duelTargetMarkerX;
    private final double duelTargetMarkerY;
    private final double duelTargetMarkerZ;

    public S2C_SyncCorpsState(int active1CooldownTicks, int active2CooldownTicks, int coreCooldownTicks,
                              int duelRemainingTicks, int loyaltyOrangeStacks,
                              boolean duelTargetMarkerActive, String duelTargetMarkerDimension,
                              double duelTargetMarkerX, double duelTargetMarkerY, double duelTargetMarkerZ) {
        this.active1CooldownTicks = active1CooldownTicks;
        this.active2CooldownTicks = active2CooldownTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.duelRemainingTicks = duelRemainingTicks;
        this.loyaltyOrangeStacks = loyaltyOrangeStacks;
        this.duelTargetMarkerActive = duelTargetMarkerActive;
        this.duelTargetMarkerDimension = duelTargetMarkerDimension == null ? "" : duelTargetMarkerDimension;
        this.duelTargetMarkerX = duelTargetMarkerX;
        this.duelTargetMarkerY = duelTargetMarkerY;
        this.duelTargetMarkerZ = duelTargetMarkerZ;
    }

    public static void encode(S2C_SyncCorpsState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.active1CooldownTicks);
        buf.writeVarInt(msg.active2CooldownTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.duelRemainingTicks);
        buf.writeVarInt(msg.loyaltyOrangeStacks);
        buf.writeBoolean(msg.duelTargetMarkerActive);
        buf.writeUtf(msg.duelTargetMarkerDimension);
        buf.writeDouble(msg.duelTargetMarkerX);
        buf.writeDouble(msg.duelTargetMarkerY);
        buf.writeDouble(msg.duelTargetMarkerZ);
    }

    public static S2C_SyncCorpsState decode(FriendlyByteBuf buf) {
        return new S2C_SyncCorpsState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public static void handle(S2C_SyncCorpsState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCorpsHudState.sync(
                        msg.active1CooldownTicks,
                        msg.active2CooldownTicks,
                        msg.coreCooldownTicks,
                        msg.duelRemainingTicks,
                        msg.loyaltyOrangeStacks,
                        msg.duelTargetMarkerActive,
                        msg.duelTargetMarkerDimension,
                        msg.duelTargetMarkerX,
                        msg.duelTargetMarkerY,
                        msg.duelTargetMarkerZ
                ));
    }
}
