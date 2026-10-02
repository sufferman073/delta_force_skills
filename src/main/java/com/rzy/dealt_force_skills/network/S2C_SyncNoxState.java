package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncNoxState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncNoxState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_nox_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncNoxState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncNoxState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int rotorCooldownTicks;
    private final int flashCharges;
    private final int flashMaxCharges;
    private final int flashRechargeTicks;
    private final int coreCooldownTicks;
    private final int corePrepTicks;
    private final int stealthTicks;
    private final int equippedToolOrdinal;

    public S2C_SyncNoxState(
            int rotorCooldownTicks,
            int flashCharges,
            int flashMaxCharges,
            int flashRechargeTicks,
            int coreCooldownTicks,
            int corePrepTicks,
            int stealthTicks,
            int equippedToolOrdinal
    ) {
        this.rotorCooldownTicks = rotorCooldownTicks;
        this.flashCharges = flashCharges;
        this.flashMaxCharges = flashMaxCharges;
        this.flashRechargeTicks = flashRechargeTicks;
        this.coreCooldownTicks = coreCooldownTicks;
        this.corePrepTicks = corePrepTicks;
        this.stealthTicks = stealthTicks;
        this.equippedToolOrdinal = equippedToolOrdinal;
    }

    public static void encode(S2C_SyncNoxState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.rotorCooldownTicks);
        buf.writeVarInt(msg.flashCharges);
        buf.writeVarInt(msg.flashMaxCharges);
        buf.writeVarInt(msg.flashRechargeTicks);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeVarInt(msg.corePrepTicks);
        buf.writeVarInt(msg.stealthTicks);
        buf.writeVarInt(msg.equippedToolOrdinal);
    }

    public static S2C_SyncNoxState decode(FriendlyByteBuf buf) {
        return new S2C_SyncNoxState(
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

    public static void handle(S2C_SyncNoxState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNoxHudState.sync(
                        msg.rotorCooldownTicks,
                        msg.flashCharges,
                        msg.flashMaxCharges,
                        msg.flashRechargeTicks,
                        msg.coreCooldownTicks,
                        msg.corePrepTicks,
                        msg.stealthTicks,
                        msg.equippedToolOrdinal
                ));
    }
}
